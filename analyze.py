import google.generativeai as genai
import os

genai.configure(api_key=os.environ["GEMINI_API_KEY"])
model = genai.GenerativeModel('gemini-1.5-flash')

def analyze_image(path):
    print(f"Analyzing {path}...")
    sample_file = genai.upload_file(path=path)
    response = model.generate_content([sample_file, "Extract all text from this image exactly as it appears. Highlight any badges, titles, subjects, dates, and times."])
    print(response.text)

analyze_image("/Users/towkirahmed/.gemini/antigravity/brain/80a621a3-1e01-4770-aa56-b0b02d5508df/.user_uploaded/media_1790218530157.jpg")
analyze_image("/Users/towkirahmed/.gemini/antigravity/brain/80a621a3-1e01-4770-aa56-b0b02d5508df/.user_uploaded/media_1790218526072.jpg")
