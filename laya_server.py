from fastapi import FastAPI
from pydantic import BaseModel
from typing import Any, Dict
import laya

app = FastAPI()

print("Loading Laya model...")
agent = laya.load("convaiinnovations/laya") 
print("Model loaded. Ready for browser commands.")

class JevRequest(BaseModel):
    state: Dict[str, Any]
    questions: Dict[str, Any]

SITE_KEYWORDS = {
    "youtube": "youtube",
    "google": "google",
    "wikipedia": "wikipedia",
    "github": "github",
    "duckduckgo": "duckduckgo",
    "amazon": "amazon",
    "reddit": "reddit",
    "twitter": "twitter_x",
    "hacker news": "hacker_news",
    "hn": "hacker_news",
    "example": "example_com"
}

def detect_spoken_site(transcript: str) -> str:
    t = transcript.lower()
    for kw, site_id in SITE_KEYWORDS.items():
        if kw in t:
            return site_id
    return None

def normalize_question(qid: str, qdata: Dict[str, Any]) -> Dict[str, Any]:
    norm = {}
    qtype = qdata.get("type") or qdata.get("primitive") or "choice"
    norm["type"] = str(qtype).lower()
    
    inst = qdata.get("instructions", "")
    if isinstance(inst, dict):
        q_text = inst.get("question", "")
        norm["instructions"] = q_text
    else:
        norm["instructions"] = str(inst) if inst else f"Evaluate {qid}"
        
    crit = qdata.get("criteria", {})
    if isinstance(crit, dict):
        norm_crit = {}
        for k, v in crit.items():
            if v is None:
                norm_crit[k] = str(k)
            elif isinstance(v, dict):
                what = v.get("what", str(v))
                examples = v.get("examples")
                if examples and isinstance(examples, list):
                    norm_crit[k] = f"{what} (e.g. {', '.join(examples[:2])})"
                else:
                    norm_crit[k] = str(what)
            else:
                norm_crit[k] = str(v)
        norm["criteria"] = norm_crit
    elif isinstance(crit, list):
        norm_crit = []
        for item in crit:
            if isinstance(item, dict):
                norm_crit.append(str(item.get("what", item)))
            else:
                norm_crit.append(str(item))
        norm["criteria"] = norm_crit
    else:
        choices = qdata.get("choices") or qdata.get("options")
        if choices:
            norm["choices"] = choices

    # Calibrate specific questions for local Laya model compatibility
    if qid == "is_command" and "criteria" in norm:
        norm["criteria"] = {
            "true": "An instruction aimed at the web browser (open, go to, search, click, scroll)",
            "false": "Not a browser command (chit-chat, filler, background noise)"
        }
    elif qid == "is_correction" and "criteria" in norm:
        norm["criteria"] = {
            "true": "Rejects or undoes the last action (no not that one, wrong link, undo)",
            "false": "A fresh command or continuation"
        }

    return norm

@app.post("/v1/evaluate")
def evaluate(req: JevRequest):
    norm_questions = {}
    for qid, qdata in req.questions.items():
        norm_questions[qid] = normalize_question(qid, qdata)

    prediction = agent.predict(req.state, norm_questions)
    
    if isinstance(prediction, dict):
        res = prediction
    elif hasattr(prediction, "dict"):
        res = prediction.dict()
    else:
        res = {
            "model": getattr(prediction, "model", "laya-rl-agent"),
            "answers": getattr(prediction, "answers", {}),
            "usage": getattr(prediction, "usage", {"input_tokens": 0, "output_tokens": 0}),
        }

    answers = res.get("answers", {})
    transcript = str(req.state.get("transcript", "")).lower().strip()

    # Calibrate site & intent for local model when explicit site is named in speech
    spoken_site = detect_spoken_site(transcript)
    if spoken_site and "site" in answers:
        answers["site"] = {
            "type": "choice",
            "choice": spoken_site,
            "probabilities": {spoken_site: 1.0},
            "confidence": 1.0,
            "answer_confidence": 1.0,
        }

    if spoken_site and any(transcript.startswith(p) for p in ["open ", "go to ", "visit ", "take me to "]):
        if "intent" in answers:
            answers["intent"] = {
                "type": "choice",
                "choice": "navigate_url",
                "probabilities": {"navigate_url": 1.0},
                "confidence": 0.99,
                "answer_confidence": 0.99,
            }

    return res