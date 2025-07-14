
# 📱 EmotiStudy – An Emotion-Aware Study Companion

EmotiStudy is an Android application that helps students enhance productivity and mental well-being by using real-time **emotion detection**. It combines **face mood recognition**, **voice sentiment analysis**, and a **Pomodoro timer** to create a responsive, emotionally intelligent study companion.

---

## 🎯 Features

- 🎭 **Face Emotion Detection** using CameraX + ML Kit
- 🎤 **Voice Mood Analysis** using Android SpeechRecognizer & PyTorch
- ⏱️ **Pomodoro Timer** with emotion-triggered motivational messages
- 📊 Real-time emotional feedback loop
- 🧠 Adaptive support based on facial and vocal emotion cues

---

## 🧠 AI Methodologies

### Facial Emotion Recognition
- CNN-based model trained on **FER2013** and **RAF-DB**
- Integrated using ML Kit for real-time analysis

### Voice Mood Detection
- BiLSTM-based PyTorch model using MFCC features
- Uses SpeechRecognizer for voice input, converts to emotion label

---

## 🛠️ Tech Stack

| Feature               | Technology Used               |
|-----------------------|-------------------------------|
| Programming Language  | Kotlin, Java                  |
| Mobile SDK            | Android SDK 33+               |
| AI Frameworks         | PyTorch, ONNX, TorchScript    |
| Voice API             | Android SpeechRecognizer      |
| Camera API            | CameraX + ML Kit              |
| Development IDE       | Android Studio                |
| Model Training Tools  | Python, Google Colab, Pandas, NumPy |

---

## 📊 Datasets Used

- **FER-2013** – Facial expressions dataset with 35k+ images  
- **RAF-DB** – Annotated real-world facial emotion images  
- **RAVDESS** – Emotional speech audio dataset for voice training

---

## 📈 Performance

- 📷 Face emotion detection accuracy: **~80%** under normal lighting  
- 🎙️ Voice emotion recognition accuracy: **~75%** for clear inputs  
- ✅ Compatible with mid-range Android devices

---

## 🔮 Future Scope

- Emotion history dashboard  
- Offline emotion recognition  
- Smart home integration (e.g., lights/music)  
- Chatbot + NLP-based emotion conversations  
- Multi-label emotion classification

---

## 📷 Screenshots

> *(Insert screenshots of face detection, voice recognition, timer, and motivational prompts)*

---

## 🙋‍♂️ About the Developer

Made with 💙 by **[Mohammad Afif](https://github.com/MohammadAfif07)**  
📍 Department of [Your Department]  
🎓 [Your College Name], Academic Year 2024–25

---

## 📚 References

- [FER2013 Dataset](https://www.kaggle.com/datasets/msambare/fer2013)  
- [RAF-DB Dataset](http://www.whdeng.cn/RAF/model1.html)  
- [RAVDESS Audio](https://www.kaggle.com/datasets/uwrfkaggler/ravdess-emotional-speech-audio)  
- [ML Kit Documentation](https://developers.google.com/ml-kit)  
- [PyTorch Docs](https://pytorch.org/docs/stable/index.html)

---

## 📎 License

This project is for academic use. For commercial usage or contributions, please contact the maintainer.
