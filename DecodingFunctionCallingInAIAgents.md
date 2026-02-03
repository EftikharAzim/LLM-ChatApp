How Does Function Calling Really Work in AI Agents? Decoding the LLM Illusion


Welcome back! Been a while since my last article…work had me tied up, but hey, I’m back! 😎

Lately (well, not just lately…since way back in September 2024), AI agents have been stealing the spotlight. If you’ve been paying attention, you’ve probably heard the buzz. But what are AI agents, really? Let’s cut through the noise: AI agents are just loops or structured thought processes, paired with an LLM and hooked up to external tools to fetch real-world data giving them supercharged abilities to perform tasks or enhance responses.

Now, as this whole agentic era picks up steam, there’s a hot topic making waves: Function Calling. And guess what? A lot of people assume that LLMs just call functions based on user queries and spit out customized responses. If you’re one of those people… hate to break it to you, but that’s a big misconception.

But don’t worry, I’ve got you covered. Let’s clear up two big myths right away:

Function calling as a “single-pass” process? Total illusion.
Only new LLMs can do function calling? Nope! Any LLM can do it. The only difference? Models fine-tuned for function calling just do it better. No need to just take my word for it, I’m about to prove it with some example code using a lightweight LLM. Let’s dive in!

Inside AI Agent | Source: Author
How Do LLMs Actually Work?
Alright, let’s get one thing straight LLMs (Large Language Models) are designed to generate. Whether it’s text, images, audio, or even video, their architecture is built just for that. So no, they’re not the ones actually executing function calls.

Now, you might be wondering — if LLMs aren’t doing the function calling, then how do we get customized responses?

Before I break it down, let me just say this “Function Calling” is a terrible name. Hate to admit it, but it’s misleading as hell. It makes people think LLMs are actively calling functions like a traditional program. Nope. If anything, a better name would be “Function Generation.”

But I get why they called it function calling. The trick is, LLMs are generating functions, and with some backend tweaks, you can unlock all sorts of crazy possibilities, like real-time context rich responses or modifying external services dynamically. That’s what gives the illusion that LLMs are doing all the work.

Now that we’ve cleared that up, let’s dive into how it really works.

What Does Function Calling Look Like?
Alright, let’s break it down. Imagine you hop onto Gemini and type:

“Hey, can you find me flights from Miami to Las Vegas?” (Solid vacation spot, by the way…)

Press enter or click to view image in full size

Illusion | Source: Author
Now, Gemini shows you something like “Searching…” or, if you’ve enabled chain-of-thought reasoning, it might even display a text similar to blue box that hints at its thought process.

So, what’s shown here?

First, the model receives your request.
It decides to use the flight API tool.
Then, it calls the API, fetches flight details, and responds with something like: “Here are the available flights.”
Sounds straightforward, right? But here’s the big illusion.

There’s no model actually waiting for the API response in real time it just looks that way. With clever UI design and optimized backend hardware, the process is seamless, masking what’s really happening under the hood. It’s all about perception. And that, my friend, is how function calling appears to work.

How Does It Really Work?
Alright, let’s break this down step by step:

Press enter or click to view image in full size

Actual Working | Source: Author
LLM gets the user query…Like asking for flights.
LLM generates a function/tool call in its output…But here’s the kicker, this is just text generation, not actual execution.
A parser from automation steps in — the system detects when the LLM has generated a complete function call (e.g., when it hits something like <function_call>).
Automation takes over – The system intercepts that function call text.
The real action happens – The backend infrastructure makes the actual API request—not the LLM itself.
Once the data comes back, the system merges: The original context + The user’s query + The newly fetched data
This combined info is fed back into the LLM as a new prompt.
The LLM then generates a response, making it look like it seamlessly pulled the data itself.
Bottom line? The LLM isn’t “using tools” in the way most people think. It’s not executing code or making API calls , it’s just spitting out structured text that tells the reasoning loop / automated system what to do. The real magic happens behind the scenes, but with some UI tricks, it feels like the LLM is doing all the work. Big distinction. Big illusion. But now you know the truth. 😉

Can a Normal LLM Handle Function Calls?
Short answer? YES! 💯

It doesn’t matter which LLM you’re using all you need is for it to generate structured text in the right format to enable function calling. If that sounds confusing, don’t worry I’ll even pull up a quote straight from the LangChain documentation to back this up.

Remember, while the name “tool calling” implies that the model is directly performing some action, this is actually not the case! The model only generates the arguments to a tool, and actually running the tool (or not) is up to the user. [REF]

So, What’s the Difference Between a Regular LLM and One Labeled for Function Calling (like those on Hugging Face)? The main difference? Function-enabled LLMs are fine-tuned to consistently generate structured function call outputs in a predictable way.

Here’s what sets them apart:

Output Format Training

Standard LLMs? They generate free-form text.
Function-enabled LLMs? They’re trained to output structured function calls (JSON, XML, or other schemas).
Instruction Following

Function-ready models are way better at following instructions on when and how to use functions.
They can reliably generate valid parameter names and values based on function specs.
System Recognition

Their outputs are structured in a way that external systems can easily parse and extract function parameters. (i’ll show you the proof…)
Format Adherence

They understand syntax rules like opening/closing tags and brackets, ensuring function calls are properly formatted.
Model Architecture

The core architecture? Same as any other LLM.
The real difference? Training data and fine-tuning objectives.
For example, an LLM labeled “function calling” on Hugging Face has been specifically trained to:

Detect when a function should be used based on user input.
Generate properly formatted function calls with valid parameters.
Seamlessly continue the conversation after retrieving function results.
Again! The actual execution of these function calls doesn’t happen inside the model itself it happens externally, in an orchestration layer. The model just outputs structured text that makes function calling work efficiently.

Get Tarun Reddi’s stories in your inbox
Join Medium for free to get updates from this writer.

Enter your email
Subscribe
So yeah, any LLM can technically do function calling. It’s just that some are trained to do it way better. 🚀

Let Me Show You How It’s Done!
Alright, buckle up! I’m about to use Gemma3 1B to generate a weather report just like a news broadcast. 📰🌦️ And since we need real-time weather data for that, you might be thinking, “Well, this is where specialized models with built-in function calling abilities come in.” Nope! This can be done by instructing the LLM to generate structured JSON responses via clear system prompts. And any LLM can be steered to create parseable outputs with the right instructions and examples. Let’s break it down:

1. Environment & Dependencies
LangChain: Acts as an abstraction layer for smooth LLM interaction.
Ollama: Local deployment system for open-source models.
Gemma 3 1B: A lightweight LLM from Google.
Standard Libraries: For handling JSON parsing, HTTP requests, and dates.
from langchain_ollama import ChatOllama
from langchain.schema import SystemMessage, HumanMessage
import json
import requests
from datetime import datetime

# Ollama Model
MODEL_NAME = "gemma3:1b"

# Weather API (No Sign-Up Required)
WEATHER_API_URL = "https://api.open-meteo.com/v1/forecast"
2. Function Specification System
We’ve defined the available functions, which serves as a clear schema just like commercial function calling systems, except here it’s more for documentation.

# Define function descriptions for LangChain
FUNCTIONS = {
    "get_weather": {
        "description": "Fetches real-time weather data for a given city.",
        "parameters": {
            "city": {"type": "string", "description": "City name"}
        }
    }
}
3. The Critical System Prompt: Engineering Structured Output
This is where the magic happens:

Role definition: We give the model a specific identity.
Output format specification: We tell it the exact JSON structure we need.
Few-shot examples: We show it how the model should respond to typical queries.
Conditional behavior: The model knows when to use structured output vs. natural language.
Simplicity: We keep it lean, only including the necessary JSON structure to make it more reliable.
# Define a structured prompt to force JSON function calls
SYSTEM_PROMPT = """
You are WeatherBot, an AI weather reporter.
When the user asks about weather, respond in this structured JSON format:

{
    "function": "get_weather",
    "parameters": {
        "city": "<city_name>"
    }
}

Examples:
User: What is the weather like in Berlin today?
Assistant: {"function": "get_weather", "parameters": {"city": "Berlin"}}

User: How's the weather in Tokyo?
Assistant: {"function": "get_weather", "parameters": {"city": "Tokyo"}}

For any non-weather questions, respond normally as a helpful assistant.
"""
4. Advanced Response Parsing Logic
We’re handling multiple output formats the LLM might generate:

Markdown handling: We grab JSON from within json code blocks.
Direct JSON parsing: If no code block is found, we try to parse it as raw JSON.
Graceful error handling: If parsing fails, we just send back the original response.
Default parameter values: If the city’s missing, we use “unknown city” as the default.
Function registry pattern: We conditionally route to the right function.
This needed some fine-tuning because, this model wasn’t designed for function calling.

def execute_function(response, model):
    """Extract and execute function calls from model response."""
    try:
        # Extract JSON from code block if present
        if "```json" in response and "```" in response:
            # Find the JSON content between the code block markers
            start = response.find("```json") + 7
            end = response.find("```", start)
            json_str = response[start:end].strip()
        else:
            json_str = response.strip()
        
        parsed_response = json.loads(json_str)
        function_name = parsed_response["function"]
        parameters = parsed_response.get("parameters", {})

        if function_name == "get_weather":
            city = parameters.get("city", "unknown city")
            weather_data, city_used = get_weather(city)
            return generate_weather_report(weather_data, city_used, model)
        else:
            return f"❌ Unknown function '{function_name}'"
    
    except json.JSONDecodeError:
        # If not a function call, return the original response
        return response
5. Weather API Integration & Data Handling
The get_weather function manages the API integration:

City-to-coordinate mapping: Simple table for city lookups.
Fallbacks: If the city isn’t recognized, we use London coordinates.
Error handling: We handle API failures gracefully.
Parameter customization: We request specific weather data.
def get_weather(city):
    """Fetch detailed weather data from Open-Meteo API."""
    params = {
        "latitude": 51.5074,  # Default London
        "longitude": -0.1278,
        "current": "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m",
        "daily": "temperature_2m_max,temperature_2m_min,precipitation_sum",
        "timezone": "auto",
        "forecast_days": 1
    }

    city_coords = {
        "London": (51.5074, -0.1278),
        "New York": (40.7128, -74.0060),
        "Paris": (48.8566, 2.3522),
        "Tokyo": (35.6762, 139.6503),
        "Sydney": (-33.8688, 151.2093),
        "Berlin": (52.5200, 13.4050),
        "Cairo": (30.0444, 31.2357),
        "Moscow": (55.7558, 37.6173),
        "Beijing": (39.9042, 116.4074),
        "Rio": (-22.9068, -43.1729)
    }

    if city in city_coords:
        params["latitude"], params["longitude"] = city_coords[city]
    else:
        # Default to London if city not found
        city = "Unknown location (defaulting to London)"

    response = requests.get(WEATHER_API_URL, params=params)
    if response.status_code == 200:
        return response.json(), city
    else:
        return None, city
6. Data Transformation Pipeline
Before sending the weather data back to the LLM, we process it through two transformation functions:

Convert numeric weather codes to human-readable descriptions.
Extract relevant fields from complex API responses.
Handle missing data with ease.
def weather_code_to_description(code):
    """Convert WMO weather code to text description."""
    weather_codes = {
        0: "Clear sky",
        1: "Mainly clear",
        2: "Partly cloudy",
        3: "Overcast",
        45: "Fog",
        48: "Depositing rime fog",
        51: "Light drizzle",
        53: "Moderate drizzle",
        55: "Dense drizzle",
        56: "Light freezing drizzle",
        57: "Dense freezing drizzle",
        61: "Slight rain",
        63: "Moderate rain",
        65: "Heavy rain",
        66: "Light freezing rain",
        67: "Heavy freezing rain",
        71: "Slight snow fall",
        73: "Moderate snow fall",
        75: "Heavy snow fall",
        77: "Snow grains",
        80: "Slight rain showers",
        81: "Moderate rain showers",
        82: "Violent rain showers",
        85: "Slight snow showers",
        86: "Heavy snow showers",
        95: "Thunderstorm",
        96: "Thunderstorm with slight hail",
        99: "Thunderstorm with heavy hail"
    }
    return weather_codes.get(code, "Unknown weather condition")

def format_weather_data(weather_data, city):
    """Format raw weather data into a readable string."""
    if not weather_data:
        return f"Unable to retrieve weather data for {city}."
    
    current = weather_data.get("current", {})
    daily = weather_data.get("daily", {})
    
    current_temp = current.get("temperature_2m", "N/A")
    feels_like = current.get("apparent_temperature", "N/A")
    humidity = current.get("relative_humidity_2m", "N/A")
    weather_code = current.get("weather_code", 0)
    weather_desc = weather_code_to_description(weather_code)
    wind_speed = current.get("wind_speed_10m", "N/A")
    wind_direction = current.get("wind_direction_10m", "N/A")
    precipitation = current.get("precipitation", 0)
    
    max_temp = daily.get("temperature_2m_max", [0])[0] if "temperature_2m_max" in daily and daily["temperature_2m_max"] else "N/A"
    min_temp = daily.get("temperature_2m_min", [0])[0] if "temperature_2m_min" in daily and daily["temperature_2m_min"] else "N/A"
    
    formatted_data = {
        "city": city,
        "current_temp": current_temp,
        "feels_like": feels_like,
        "conditions": weather_desc,
        "humidity": humidity,
        "wind_speed": wind_speed,
        "wind_direction": wind_direction,
        "precipitation": precipitation,
        "max_temp": max_temp,
        "min_temp": min_temp,
        "time": datetime.now().strftime("%A, %B %d at %I:%M %p")
    }
    
    return formatted_data
7. Secondary Prompting: Natural Language Generation
Once the data’s formatted, we send a second prompt to generate the weather report:

It uses the formatted API data to build a natural language report.
The model adopts the role of a “professional weather reporter.”
It also includes the raw data and a simple request for a report.
Error fallback: If the model messes up, it generates a basic report instead.
# Define a prompt for generating weather reports
WEATHER_REPORT_PROMPT = """
You are WeatherBot, a professional weather reporter. Convert the following weather data into a natural, engaging weather report like a meteorologist would deliver. Include the temperature and be conversational.

Weather Data: {weather_data}

City: {city}
"""

def generate_weather_report(weather_data, city, model):
    """Generate a natural language weather report using the model."""
    formatted_data = format_weather_data(weather_data, city)
    
    messages = [
        SystemMessage(content=WEATHER_REPORT_PROMPT.format(
            weather_data=json.dumps(formatted_data, indent=2),
            city=city
        )),
        HumanMessage(content=f"Please give me a weather report for {city}.")
    ]
    
    try:
        response = model.invoke(messages).content
        return response
    except Exception as e:
        # Fallback to basic report if model generation fails
        if isinstance(formatted_data, dict):
            return f"Currently in {formatted_data['city']}, it's {formatted_data['current_temp']}°C with {formatted_data['conditions'].lower()}. The high today will be {formatted_data['max_temp']}°C and the low will be {formatted_data['min_temp']}°C."
        else:
            return formatted_data  # Return the error message
8. Main Application Loop & Orchestration
Here’s how the whole process runs:

The LLM is initialized using LangChain.
We create a continuous interaction loop.
The system prompt is applied to each user query.
It checks if the response looks like a function call.
The function parsing logic is executed if needed.
We display user-friendly status messages during the process.
def main():
    """Run the weather reporter AI."""
    model = ChatOllama(model=MODEL_NAME)
    
    print("🌦️ Welcome to AI Weather Reporter! Ask about the weather anywhere.\n")
    
    while True:
        user_input = input("You: ")
        if user_input.lower() in ['exit', 'quit', 'bye']:
            print("AI Weather Reporter: Goodbye! Have a nice day! 👋")
            break
            
        # Send input to LangChain-based Ollama model
        messages = [
            SystemMessage(content=SYSTEM_PROMPT),
            HumanMessage(content=user_input)
        ]

        print("\nThinking...")
        raw_response = model.invoke(messages).content
        
        # Check if the response looks like a function call
        if "{" in raw_response and "function" in raw_response:
            print("🔍 Fetching weather data...")
            final_response = execute_function(raw_response, model)
        else:
            final_response = raw_response
            
        print(f"\nAI Weather Reporter: {final_response}\n")

if __name__ == "__main__":
    main()
Validation Time!
Alright, let’s put this to the test and see if we really pulled off the function calling here. We’re going to verify the model’s response by making a manual API call. Here’s the link: [Click here!].

Note: I used the latitude and longitude for New York here. If you’re testing it, make sure to update the coordinates for the city you’re testing.

Now, check out the comparison between the model’s response and the API’s response. You’ll see some clear similarities between the two. From both responses, we can confirm that everything lines up just right. 🔍


API Response
Press enter or click to view image in full size

Agent Response
Current temperature (8.0°C) ✓
Apparent “feels like” temperature (3.0°C) ✓
Humidity (80%) ✓
Wind speed (25.2 km/h) ✓
Weather condition (overcast, which is correct for weather code 3) ✓
Maximum temperature for the day (14.7°C) ✓
Current time reference to March 17th ✓
So yep, the model absolutely nailed the function calling here! 💥

Why This Works
Instruction Tuning: Modern LLMs are great at following instructions. The system prompt capitalizes on that by providing clear, structured guidance for formatting.

In-Context Learning: We give the LLM a few examples, and it learns how to behave without needing more training. This helps it consistently generate valid JSON.

Robust Parsing Logic:
We cover all bases in parsing the LLM’s output:

Raw JSON
JSON in code blocks
Non-JSON responses when the query isn’t weather-related
Two-Stage Prompting Architecture:
We split the process into two stages:

First prompt: Focuses on extracting entities and choosing the right function.
Second prompt: Takes care of generating the natural language response.
Error Handling:
We’ve built in multiple fallback mechanisms:

Default coordinates for unknown cities.
Default responses when API calls fail.
A templated weather report if the second prompt fails.
The original LLM response if JSON parsing fails.
Limitations & Considerations
Even with all this coolness, it’s not perfect:

Reliability: Sometimes the LLM doesn’t stick to the expected JSON format.
Complexity ceiling: As the function schema gets more complex, reliability can drop.
Parameter extraction: City extraction is easy, but more complex parameters? That’s trickier.
Error handling: It’s not as standardized as in native function calling.
Conclusion
Alright, let’s wrap this up! We’ve taken a fun dive into the world of LLMs and function calling. From clearing up some common myths to showing how even lightweight models can pull off some pretty cool tricks like generating real-time weather reports we’ve explored just how these models generate function calls and work behind the scenes. It’s all about giving LLMs the right instructions and tools, which, with a little magic from frameworks, can lead to some pretty impressive results!

So, what’s next? Well, there’s a lot more to come! We’re just getting started with the possibilities of AI agents and function generation. There’s always room for improvement and new features, so stick around for more updates! I’m excited to keep sharing what’s next, so make sure to stay tuned. And hey, thanks for reading there’s plenty more coming your way here at Teen Different! 🚀✨

Loved💓 what you read? Give it a clap, drop a comment, or fire away with questions. Let’s revolutionize the tech space together! If you’re curious about AI opportunities, I’m always open to collaborate and innovate. Because at the end of the day, sharing this journey with you is what truly drives me.

You can check out the code file and diagrams (I used Excalidraw for the visuals) right here!



Other recommended reads:

Why AGI Isn’t a One-Model Wonder (and Never Will Be)
Beyond LoRA: A Comprehensive Guide to Efficient Model Fine-Tuning
Why LoRA Struggles with Object Detection (And What I Learned the Hard Way)
Stay curious, and as always, have fun experimenting! 👏

References
Hugging Face. (n.d.). Function Calling Guide. Retrieved from https://huggingface.co/docs/hugs/en/guides/function-calling
Langchain. (n.d.). Tool Calling in Langchain. Retrieved from https://python.langchain.com/docs/how_to/tool_calling/
Whitepaper Agents. (n.d.). Kaggle Whitepaper on Agents. Retrieved from https://www.kaggle.com/whitepaper-agents
Tools

Excalidraw: For flow digrams [Click here!]
langchain_ollama & langchain.schema: LLM interaction framework
json & requests: Data processing and API calls
datetime: Time formatting
Ollama: Local model deployment service
Gemma 3 1B: Open-source LLM from Google
Open-Meteo API: Free weather data service (no key required)