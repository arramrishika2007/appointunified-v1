# Groq API Integration Guide

## Overview
The AppointUnified application now integrates **Groq API** (`llama3-70b-8192` model) for two key features:

1. **V9.4 — AI-Powered Schedule Optimization Suggestions**
   - Analyzes professional's appointment patterns
   - Generates 3 actionable schedule improvements via Groq
   - Stored in `ai_suggestions` table for professional dashboard display

2. **FEATURE B — RAG-Powered System Chat**
   - Floating chatbot widget answers platform-specific questions
   - Uses Retrieval-Augmented Generation (RAG) with platform knowledge context
   - Prevents leakage of sensitive data (medical records, personal info)

## Setup Instructions

### 1. Backend Configuration

**Option A: Environment Variable**
```bash
export GROQ_API_KEY=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
```

**Option B: Application Properties**
Add to `application.properties`:
```properties
groq.api.key=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
groq.api.model=llama3-70b-8192
```

Or `application.yml`:
```yaml
groq:
  api:
    key: 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
    model: llama3-70b-8192
```

### 2. Restart Backend
```bash
cd backend
./gradlew bootRun
```

## Service Architecture

### GroqService
**File:** `backend/src/main/java/com/appointunified/service/GroqService.java`

Core methods:
- `generateCompletion(String prompt)` — Basic LLM completion
- `generateCompletion(String prompt, int maxTokens, float temperature)` — Advanced options
- `generateScheduleSuggestions(String professionalData)` — Optimized for schedule analysis
- `generateChatResponse(String question, String ragContext)` — RAG chatbot queries
- `isConfigured()` — Check if API key is set

### AnalyticsService (Enhanced)
**File:** `backend/src/main/java/com/appointunified/service/AnalyticsService.java`

New methods:
- `generateScheduleSuggestionsForProfessional(UUID professionalId)` — Async Groq integration
- Parses JSON response and stores in `ai_suggestions` table
- Checks for duplicate suggestions (once per day max)

### SystemChatService (New)
**File:** `backend/src/main/java/com/appointunified/service/SystemChatService.java`

Methods:
- `answerQuestion(User, String question, String contextType, UUID contextId)` — RAG chatbot
- Builds context from platform knowledge + user-specific data
- Returns `SystemChatResponse` with answer and status

## API Endpoints

### System Chat
```
POST /api/system-chat/ask
Query Params:
  - question (required): User's question about the platform
  - contextType (optional): 'general' | 'professional' | 'booking' (default: 'general')
  - contextId (optional): UUID of professional or appointment for context

Example:
POST /api/system-chat/ask?question=How%20do%20I%20book%20an%20appointment&contextType=general

Response:
{
  "question": "How do I book an appointment?",
  "answer": "...",
  "status": "SUCCESS"
}
```

### Analytics (with AI suggestions)
```
GET /api/analytics/slot-suggestions/{professionalId}
Auth: PROFESSIONAL (self) / ADMIN

Response: List<AISuggestionResponse> with title, description, expectedImpact, etc.
```

## Database Schema Changes

### New Tables Created in V14__analytics.sql
- `ai_suggestions` — Stores Groq-generated schedule optimization suggestions
  - Fields: professional_id, suggestion_title, suggestion_description, expected_impact, suggestion_data (JSON), generated_at, is_active

## Scheduled Jobs (Recommended)

Add to your scheduler configuration to regenerate suggestions daily:

```java
@Scheduled(cron = "0 0 2 * * *") // 2 AM daily
public void regenerateAISuggestionsForAllProfessionals() {
    professionalService.getAllActiveProfessionals()
        .forEach(prof -> analyticsService.generateScheduleSuggestionsForProfessional(prof.getId()));
}
```

## Frontend Integration

### API Client
**File:** `frontend/lib/api.ts`

```typescript
export const systemChatApi = {
  askQuestion: (question: string, contextType?: string, contextId?: string) =>
    api.post('/system-chat/ask', null, { params: { question, contextType: contextType || 'general', contextId } }),
}
```

### Types
**File:** `frontend/types/index.ts`

```typescript
export interface SystemChatMessage {
  question: string
  answer: string
  status: 'SUCCESS' | 'ERROR' | 'OFFLINE'
  timestamp?: string
}
```

### Suggested UI Component
```typescript
// Floating chat widget in layout
<ChatWidget 
  onAsk={(q) => systemChatApi.askQuestion(q)}
  contextType="general"
/>
```

## Error Handling

### GroqService Failover
If Groq API is unavailable:
- `isConfigured()` returns false
- Services gracefully degrade (no suggestions generated, chat returns "offline" message)
- User experience is not broken

### Rate Limiting
- Groq free tier has rate limits (monitor usage)
- Suggestion generation: once per professional per day
- Chat responses: cache common questions in frontend localStorage
- Implement request queuing if needed

## Performance Considerations

### Response Times
- Groq API typically takes 1-3 seconds per request
- Wrap in async/background job for suggestions (don't block API response)
- Cache chat responses for 1 hour on frontend

### Token Usage
- Groq free tier: Monitor API usage at https://console.groq.com
- Max tokens set to 1024 for suggestions, 800 for chat
- Implement usage monitoring/alerting

## Testing

### 1. Test GroqService Connectivity
```bash
curl -X POST "http://localhost:8080/api/system-chat/ask?question=hello"
```

### 2. Test Schedule Suggestions
```bash
# Trigger suggestions generation (would normally be @Scheduled)
# Can add manual endpoint for testing
POST /api/analytics/generate-suggestions/{professionalId}
```

### 3. Test Chat Contextual Q&A
```bash
curl -X POST "http://localhost:8080/api/system-chat/ask?question=How%20do%20I%20cancel&contextType=booking&contextId={appointmentId}"
```

## Monitoring & Logging

GroqService logs:
- API calls and responses at DEBUG level
- Errors at ERROR level
- Configuration status at WARN level

Example log entries:
```
DEBUG: Calling Groq API with prompt: [...]
INFO: Generated and stored AI suggestions for professional=abc-123
ERROR: Error calling Groq API: Connection timeout
WARN: Groq API key not configured, returning empty response
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Groq API key not configured" | Set `GROQ_API_KEY` environment variable or `groq.api.key` in properties |
| Empty Groq responses | Check API key validity at https://console.groq.com |
| Chat returns "offline" | Verify API key, check network connectivity |
| Suggestions not generated | Verify `ai_suggestions` table exists, check logs for errors |
| Slow responses | Groq API can be slow; implement response caching |

## Next Steps

1. **Deploy with proper env variable:** Ensure `GROQ_API_KEY` is set on Railway backend
2. **Implement frontend chat widget:** Add floating chat bubble to all pages
3. **Set up scheduled job:** Daily suggestion regeneration for all professionals
4. **Monitor usage:** Watch Groq API console for rate limits and costs
5. **Gather feedback:** Iterate on suggestion quality and chat accuracy

## API Key Security

⚠️ **IMPORTANT:** The API key provided is for development/testing only.
- Rotate the key periodically
- Never commit to public repositories
- Use environment variables or secrets management (Railway Secrets)
- Monitor key usage for unauthorized access

---

**Integration Date:** April 7, 2026
**API Model:** llama3-70b-8192
**Status:** Ready for testing and deployment
