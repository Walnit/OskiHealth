from transformers import pipeline
model = pipeline('sentiment-analysis',model="cardiffnlp/twitter-roberta-base-sentiment-latest",tokenizer="cardiffnlp/twitter-roberta-base-sentiment-latest")
while True: print(model(input("Enter input: ")))
