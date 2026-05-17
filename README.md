# Alert Hound

> Turn raw logs into clear, actionable incidents, automatically!

Alert Hound is a system that watches your application logs and tells you when something is going wrong, and more importantly, helps explain *why*.

Instead of manually scanning logs, you get a clean incident like:

- “Payment service is failing”
- “Error rate suddenly increased”
- “Likely cause: database connection issue”
- “Suggested action: restart or scale service”

<br/>

<p align="center">
  <img src="https://github.com/user-attachments/assets/e4aba718-0243-4fdf-a504-dd2e5363f016" width="900"/>
  <br/>
  <em>Overview of Alert Hound dashboard</em>
</p>

---

## Why this exists

In real systems:

- Logs are massive  
- Errors are noisy  
- Debugging takes time  

This project explores a simple idea:

> What if the system could detect problems and explain them on its own?

<br/>

<p align="center">
  <img src="https://github.com/user-attachments/assets/541711ab-0fc2-4210-bdb8-6d34a6aac99a" width="900"/>
  <br/>
  <em>Logs being sent into the system</em>
</p>

---

## High Level Flow

1. A service sends a log  
2. The system collects and organizes it  
3. It keeps watching for unusual patterns  
4. When something looks wrong, it creates an incident  
5. An AI Agent looks at the situation  
6. It summarizes what’s going on and suggests a possible fix  
7. You see a clean, human-readable incident instead of raw logs as shown below  

<br/>

<p align="center">
  <img src="https://github.com/user-attachments/assets/e1c8756d-bbef-4440-8501-c56e4a3bfa41" width="700"/>
  <br/>
  <em>Example of a generated incident with summary and root cause</em>
</p>

<br/>

<p align="center">
  <img src="https://github.com/user-attachments/assets/671b6054-bab2-48fa-af36-6c900943b59a" width="850"/>
  <br/>
  <em>High level system flow</em>
</p>
