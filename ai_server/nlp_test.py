from transformers import pipeline
from flask import Flask, request
MODEL = pipeline('sentiment-analysis',model="cardiffnlp/twitter-roberta-base-sentiment-latest",tokenizer="cardiffnlp/twitter-roberta-base-sentiment-latest")
HTML = """<form action="/nlp"><label for="message">message:</label><input type="text" name="message"><br><br><input type="submit" value="Submit"></form>"""

app = Flask(__name__)

@app.route("/nlp", methods=["GET"])
def nlp():
    text = request.args.get('message')
    print(f"\n\n\n{text}\n\n\n")
    return MODEL(text)

@app.route("/")
def index():
    return HTML

if __name__ == "__main__":
    app.run()
#while True: print(model(input("Enter input: ")))
