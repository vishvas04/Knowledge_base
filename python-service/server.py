from fastapi import FastAPI, File, UploadFile, HTTPException
from pathlib import Path
import os
import uuid
import certifi
from tenacity import retry, stop_after_attempt, wait_exponential


from pydantic import BaseModel
from langchain.chains.retrieval_qa.base import RetrievalQA
from langchain_community.document_loaders import PyPDFLoader, TextLoader, Docx2txtLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_google_genai import GoogleGenerativeAIEmbeddings, ChatGoogleGenerativeAI
from dotenv import load_dotenv

load_dotenv()

gemini_api_key = os.getenv("GEMINI_API_KEY")
assert gemini_api_key, "GEMINI_API_KEY is missing!"

os.environ["REQUESTS_CA_BUNDLE"] = certifi.where()
os.environ["SSL_CERT_FILE"] = certifi.where()

app = FastAPI()

UPLOAD_DIR = Path("/Users/vishvasdevarasetty/uploads")
UPLOAD_DIR.mkdir(exist_ok=True, parents=True)

# Initialize embeddings correctly
embeddings = GoogleGenerativeAIEmbeddings(
    model="models/embedding-001",
    api_key=gemini_api_key,
    request_timeout=300,
    task_type="retrieval_document"
)

vector_store = None  # Global vector store

def get_loader(file_path: str, file_name: str):
    if file_name.endswith(".pdf"):
        return PyPDFLoader(file_path)
    elif file_name.endswith(".txt"):
        return TextLoader(file_path)
    elif file_name.endswith(".docx") or file_name.endswith(".doc"):
        return Docx2txtLoader(file_path)
    else:
        raise HTTPException(status_code=400, detail=f"Unsupported file format: {file_name}")

@app.post("/process-document")
async def process_document(file: UploadFile = File(...)):
    try:
        safe_filename = f"{uuid.uuid4().hex}_{file.filename}"
        file_path = UPLOAD_DIR / safe_filename

        content = await file.read()
        with open(file_path, "wb") as f:
            f.write(content)

        if not os.path.exists(file_path):
            raise HTTPException(status_code=500, detail=f"File was not saved: {file_path}")

        try:
            loader = get_loader(str(file_path), file.filename)
            docs = loader.load()
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Failed to load document: {str(e)}")

        text_splitter = RecursiveCharacterTextSplitter(chunk_size=300, chunk_overlap=50)
        splits = text_splitter.split_documents(docs)
        for i, split in enumerate(splits):
            split.metadata["chunk_id"] = i

        global vector_store
        vector_store = FAISS.from_documents(splits, embeddings)

        return {"status": "processed", "file": file.filename}

    except Exception as e:
        return {"error": str(e)}

class QuestionRequest(BaseModel):
    question: str

@app.post("/answer")
async def answer_question(request: QuestionRequest):
    question = request.question
    global vector_store

    if not vector_store:
        raise HTTPException(status_code=400, detail="No documents have been processed yet.")

    docs = vector_store.similarity_search(question, k=3)
    sources = [doc.metadata.get("source", "unknown") for doc in docs]

    llm = ChatGoogleGenerativeAI(
        model="gemini-1.5-pro-latest",
        api_key=gemini_api_key
    )

    chain = RetrievalQA.from_chain_type(
        llm=llm,
        retriever=vector_store.as_retriever(),
        return_source_documents=True
    )

    result = chain({"query": question})

    return {
        "answer": result["result"],
        "sources": sources,
        "llm_used": "gemini"
    }

@retry(stop=stop_after_attempt(5), wait=wait_exponential(multiplier=2, min=5, max=20))
def process_with_retry(text):
    return embeddings.embed_documents([text])