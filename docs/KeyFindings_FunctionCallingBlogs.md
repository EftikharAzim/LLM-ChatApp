# Key Findings: Function Calling in LLMs

This document summarizes the key insights from two comprehensive blog articles about function calling in AI agents and structural decoding for open LLMs.

---

## Blog 1: Decoding Function Calling in AI Agents - How It Really Works

> **Source**: Medium - Teen Different  
> **Focus**: Debunking myths about function calling and demonstrating how any LLM can perform function calling

### 🎯 Core Thesis

**Function calling is NOT what most people think it is.** LLMs don't actually execute functions—they generate structured text that tells an external system what to do.

---

### 🚫 Two Major Myths Debunked

| Myth | Reality |
|------|---------|
| Function calling is a "single-pass" process | It's actually a **multi-step orchestration** involving the LLM, parsers, and external systems |
| Only new/special LLMs can do function calling | **Any LLM** can do it—fine-tuned models just do it more reliably |

---

### 🔄 How Function Calling Actually Works

```
┌──────┐      ┌─────┐      ┌────────┐      ┌─────────┐      ┌─────────────┐
│ User │      │ LLM │      │ Parser │      │ Backend │      │ LLM (2nd)   │
└──┬───┘      └──┬──┘      └───┬────┘      └────┬────┘      └──────┬──────┘
   │             │             │                │                  │
   │ "Find flights Miami→Vegas"│                │                  │
   │────────────>│             │                │                  │
   │             │             │                │                  │
   │             │ JSON: {"function": "search_flights", ...}       │
   │             │────────────>│                │                  │
   │             │             │                │                  │
   │             │             │ Make API call  │                  │
   │             │             │───────────────>│                  │
   │             │             │                │                  │
   │             │             │                │ Merge: context + │
   │             │             │                │ query + API data │
   │             │             │                │─────────────────>│
   │             │             │                │                  │
   │             │             │                │    Natural       │
   │<──────────────────────────────────────────────────────────────│
   │             │             │                │ language response│
```

#### Step-by-Step Breakdown

1. **LLM receives user query** (e.g., asking for flights)
2. **LLM generates a function/tool call** as text output—NOT actual execution
3. **Parser/automation detects** when LLM has generated a complete function call
4. **Automation intercepts** the function call text
5. **Backend infrastructure** makes the actual API request—not the LLM
6. **System merges** original context + user's query + fetched data
7. **Combined info fed back** to LLM as a new prompt
8. **LLM generates final response** making it appear seamless

> **Key Insight**: The LLM isn't "using tools"—it's outputting structured text that tells the orchestration system what to do. The real magic happens behind the scenes with UI tricks creating the illusion.

---

### 🔍 Fine-tuned vs Regular LLMs for Function Calling

| Aspect | Standard LLM | Function-Enabled LLM |
|--------|--------------|---------------------|
| **Output Format** | Free-form text | Trained for structured outputs (JSON, XML) |
| **Instruction Following** | General | Better at knowing when/how to use functions |
| **System Recognition** | Variable | Outputs easily parsed by external systems |
| **Format Adherence** | May break syntax | Understands tags, brackets, proper formatting |
| **Core Architecture** | Same | Same (difference is in training data) |

---

### 💻 Practical Implementation Pattern

The blog demonstrates a complete working example using **Gemma3 1B** (a lightweight LLM) for a weather bot:

#### Key Components

1. **System Prompt Engineering**
   - Role definition ("You are WeatherBot")
   - Exact JSON structure specification
   - Few-shot examples
   - Conditional behavior rules

2. **Response Parsing Logic**
   - Handles JSON in markdown code blocks
   - Falls back to raw JSON parsing
   - Graceful error handling for non-JSON responses

3. **Two-Stage Prompting Architecture**
   - **First prompt**: Entity extraction + function selection
   - **Second prompt**: Natural language response generation

4. **Error Handling Mechanisms**
   - Default coordinates for unknown cities
   - Fallback responses for API failures
   - Templated weather report if second prompt fails

---

### ⚠️ Limitations Acknowledged

- **Reliability**: LLM may not always stick to expected JSON format
- **Complexity ceiling**: More complex schemas reduce reliability
- **Parameter extraction**: Simple parameters work well; complex ones are trickier
- **Error handling**: Less standardized than native function calling

---

## Blog 2: Structural Decoding (Function Calling) for All Open LLMs

> **Source**: Lepton AI  
> **Focus**: Technical approach using Finite State Automata (FSA) to enable function calling for ANY open-source LLM

### 🎯 Core Thesis

**Structural decoding using FSA can enable function calling for ANY LLM without fine-tuning**—by constraining the LLM's output at each token generation step.

---

### 🔧 The Problem with Current Approaches

```
                    ┌─────────────────────┐
                    │  Current Approaches │
                    └──────────┬──────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                                       ▼
┌─────────────────────┐               ┌─────────────────────┐
│ Prompt Engineering  │               │    Fine-tuning      │
└──────────┬──────────┘               └──────────┬──────────┘
           │                                     │
           ├─ ✅ Works sometimes                 ├─ ✅ Uses LLM learning
           ├─ ❌ LLM randomness                  ├─ ❌ Time-consuming
           └─ ❌ Requires retrying               └─ ❌ Hard to keep up
```

---

### ✨ Lepton AI's FSA-Based Solution

#### How It Works

1. **Build a Finite State Automata (FSA)** that defines "what outputs are legitimate"
2. **At every token generation step**, the FSA tells the LLM what tokens are valid
3. **Rejection sampling** ensures output follows the structure in a single pass

#### Key Difference from Prompt Engineering

| Prompt Engineering | FSA-Based Structural Decoding |
|-------------------|------------------------------|
| Checks output **post-mortem** | Built into **every inference step** |
| No guarantee of correct output | **Guarantees** correct format in one shot |
| May need retrying | Single-pass success |

#### Technical Mechanism

```
Example Token Flow for JSON output:
1. First token can ONLY be `{`
2. Next token must be a quoted string (function parameter name)
3. FSA continues validating at each step
4. Rejection sampling ensures only valid tokens are chosen
```

> **Key Advantage**: This doesn't require ANY changes to the existing LLM—it's applied at the inference stage.

---

### 📊 Practical Examples from the Blog

#### Example 1: Weather Bot

```python
# Input: "What's the weather like in Paris?"
# Output: {"location": "Paris, France", "unit": "celsius"}

# Input: "I am going to Munich tomorrow."
# Output: {"location": "Munich, Germany", "unit": "celsius"}
```

#### Example 2: Sentiment Analysis

```python
# Input: "pretty useful, solved my problem"
# Output: {"category": "positive"}

# Input: "Waste of money"
# Output: {"category": "negative"}
```

#### Example 3: Community Safety Check

```python
# Input: "pretty useful, solved my problem"
# Output: {"is_safe": true}

# Input: "this is the stupiest thing i have ever seen"
# Output: {"is_safe": false}
```

#### Example 4: Financial Analysis Bot

- Combines real-time financial data with LLM analysis
- Returns structured investment scores and detailed analysis

---

### 🚀 Key Benefits of Structural Decoding

| Benefit | Description |
|---------|-------------|
| **Universal Compatibility** | Works with ANY open-source LLM (Llama2, Mixtral, CodeLlama, etc.) |
| **No Fine-tuning Required** | Apply at inference time only |
| **Guaranteed Structure** | Output always follows predefined schema |
| **Single-Pass Success** | No retrying needed |
| **OpenAI API Compatible** | Easy migration from ChatGPT |

---

### 🔮 Future Potential

The structural decoding approach extends beyond JSON function calling:

- **Multi-modal inputs**: Image pixels, audio waves
- **Code generation**: Ensuring syntactically correct code
- **Mathematical proofs**: Structured logical outputs
- **Any structure** representable as a finite state machine

---

## 📋 Comparison Summary

| Aspect | Blog 1 (Teen Different) | Blog 2 (Lepton AI) |
|--------|------------------------|-------------------|
| **Approach** | Prompt engineering + orchestration | FSA-based structural decoding |
| **Guarantee** | Probabilistic (may fail) | Deterministic (always valid) |
| **Implementation** | Application layer | Inference engine layer |
| **Complexity** | Lower (any framework) | Higher (requires custom inference) |
| **Best For** | Quick prototyping, learning | Production systems, reliability |

---

## 🎓 Key Takeaways

1. **LLMs don't execute functions**—they generate structured text that external systems act upon

2. **Any LLM can do function calling** with proper prompt engineering, but fine-tuned models are more reliable

3. **Structural decoding (FSA)** provides guaranteed structured output without fine-tuning

4. **Two-stage prompting** (extraction → generation) is a powerful pattern for reliable function calling

5. **The "magic" is in the orchestration**—clever UI and backend systems create the illusion of seamless tool use

6. **Future AI agents** will increasingly rely on structured outputs to interface with existing IT systems programmatically
