from transformers import pipeline
from flask import Flask, request
import openai, os
openai.api_key = os.getenv("CGPT")

MODEL = pipeline('sentiment-analysis',model="cardiffnlp/twitter-roberta-base-sentiment-latest",tokenizer="cardiffnlp/twitter-roberta-base-sentiment-latest")
HTML = """<form action="/nlp"><label for="message">message:</label><input type="text" name="message"><br><br><input type="submit" value="Submit"></form>"""

app = Flask(__name__)

@app.route("/nlp", methods=["GET"])
def nlp():
    text = request.args.get('message')
    return MODEL(text)

@app.route("/")
def index():
    return HTML

if __name__ == "__main__":
    app.run()
#while True: print(model(input("Enter input: ")))
