from transformers import pipeline
from flask import Flask, request, abort
import openai, os

openai.api_key = os.getenv("CGPT")
MODEL = pipeline('sentiment-analysis',model="cardiffnlp/twitter-roberta-base-sentiment-latest",tokenizer="cardiffnlp/twitter-roberta-base-sentiment-latest")
app = Flask(__name__)

@app.route("/nlp", methods=["GET"])
def nlp():
    text = request.args.get('message')
    return MODEL(text)

@app.route("/chatgpt", methods=['POST'])
def chatgpt():
    data = request.get_json()
    resp = openai.ChatCompletion.create(model="gpt-3.5-turbo", messages=data, max_tokens=64)
    return resp['choices'][0]['message']['content']

if __name__ == "__main__":
    app.run()
