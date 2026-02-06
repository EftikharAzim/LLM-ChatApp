Structural Decoding (Function Calling) for all Open LLMs
Lepton AI
Lepton AI

Follow
9 min read
·
Jan 5, 2024
84




LLMs are great, but…
You want to use the awesome large language models (LLMs) to extract information as JSON texts because you want to use it in your end-to-end agent. However, the LLM is always trying to be polite — so polite that it’s… annoying. Prompt engineering doesn’t help — as much as you tell it to be specific, sometimes the LLM is still overly friendly:

Press enter or click to view image in full size

Have you encountered such frustrations?

The next frontier for AI is to build agents: with LLMs as core controllers, leveraging LLMs’ strong capabilities in understanding unstructured text inputs, and generating structured data as outputs. These outputs are designed to be directly consumable by downstream components — APIs, conventional databases, etc. The ability to generate structured outputs, instead of unstructured conversation, is key to enabling seamless integration of AI into broader applications and workflows.

We are proud to announce the general availability of the structured decoding capability for ALL the open-source models hosted on Lepton AI. Simply provide the schema you want the LLM to produce, and all our model APIs will automatically produce outputs following the schema. In addition, you can host your own LLMs with structured decoding capability without having to finetune — talk to us today at info@lepton.ai!

Here are some more details about structural decoding:

Recap: What is Structural Decoding?
OpenAI introduced the feature called “Function Calling” in the ChatGPT API. Users provide a description of the desired output structure in their requests, and ChatGPT will then return an output in JSON format following such structure, representing the “arguments” to call downstream functions.

Assuming that you are building a weather bot to understand what the user wants, like “what’s it like in San Francisco?”, so you can call a weather forecast API. You may define a structure that looks like this with two fields “location” and “format”, one indicating the city and state and one the temperature unit to use:

{
    "type": "function",
    "function": {
        "name": "get_current_weather",
        "description": "Get the current weather",
        "parameters": {
            "type": "object",
            "properties": {
                "location": {
                    "type": "string",
                    "description": "The city and state, e.g. San Francisco, CA",
                },
                "format": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "The temperature unit to use. Infer this from the users location.",
                },
            },
            "required": ["location", "format"],
        },
    }
},
A sample output from ChatGPT returns the arguments that should be used to call the `get_current_weather` function in json format:

{
    'type': 'function',
    'function': {
        'name': 'get_current_weather',
        'arguments': '{
            "location": "San Francisco",
            "format": "fahrenheit"
        }'
    }
}
This is super convenient. Can we have this with open-source models? Unfortunately, to make open-source models compatible with function calling, the conventional approach is still restricted to two approaches: prompt engineering, or fine-tuning.

Awesome libraries such as Guardrails AI, LangChain, and LLamaIndex build prompt engineering solutions (e.g. by adding system prompts) to ask the LLMs to follow the structured output format. This is successful to some extent but is still at the mercy of the LLM randomness — if LLMs do not produce ideal outputs, re-prompting and retrying is the only solution.
By collecting a set of examples following the desired structure, finetuning allows one to utilize LLMs’ strong learning capability. But, building training data is time-consuming and manpower-consuming, and with new open-source models emerging every day, it’s hard to imagine doing fine-tuning for every model every time.
We’ll describe our approach that is complementary to both existing approaches.

Lepton AI offers Structual Decoding for Any OSS model
Inspired by the classical idea of Finite State Automata (FSA), we employ a direct approach to solve the structured decoding problem: we build an FSA that tells us “what outputs are legit”, and kindly ask the LLM to produce output in the desired state space defined by the output schema, at every step of token generation. Note that this is different from prompt engineering — prompt engineering only checks the output post-mortem and does not guarantee the output to be correct in one shot. In contrast, our FSA is built into every step of the LLM inference pipeline, making sure that the output format is correct in one single pass.

More specifically, for every step of the inference process, the FSA tells us what next tokens are legit: for example, the very first token can only be { for a json output. Following that, it can only be a quoted string that is one of the function parameter names. This quickly gets more complex when we decode and generate the parameter values — but fundamentally, the FSA properly defines the valid set of tokens. Rejection sampling is then carried out for the step, ensuring that the output is a partially completed, structured output.

Note that this does not need to change ANY of the existing LLM model — it can be applied at the inference stage of any LLMs. At Lepton, we built our own fast LLM inference engines, making it particularly easy to integrate. As a result, you can bring a pre-trained model and run it on the Lepton platform — and automatically get the capability of structured decoding. Talk to us at info@lepton.ai if you are interested!

Of course, you will be able to also plug in your own system prompts or fine-tuned models specifically for function calling. In fact, rejection sampling (a slightly different version of it) has been applied in the training of LLaMA2, and we believe that it could serve as a fundamental way to accelerate fine-tuning, especially in areas where the output space is well structured — such as code generation, mathematical proof, in addition to function calling.

Get Lepton AI’s stories in your inbox
Join Medium for free to get updates from this writer.

Enter your email
Subscribe
Let’s take a look at some examples.

Example 1: Weather Bot
Let’s use the weather bot as an example: we extract information from the user’s free-form text. To do this, we simply need to define a function stub and pass it to the inference API. Lepton AI’s public API is fully compatible with the OpenAI library, so we will start with:

import os
import openai

model = "llama2-70b"  # can be any LLM model from Lepton Playground: e.g. ["mixtral-8x7b", "llama2-7b", "llama2-13b", "llama2-70b", "codellama-7b", "codellama-13b", "codellama-34b"], please see https://www.lepton.ai/playground for complete list
base_url = f"https://{model}.lepton.run/api/v1/"  # can be url of any of Lepton LLM deployments that serve your custom model
LEPTON_API_KEY=os.environ.get("LEPTON_API_KEY")

client = openai.OpenAI(base_url=base_url, api_key=LEPTON_API_KEY)
And now let’s define the weather bot:

from typing import Annotated
from leptonai.util import tool

def get_current_weather(
    location: Annotated[str, "The city and state, e.g. San Francisco, CA"],
    unit: Annotated[str,
        ("The temperature unit to use. Infer this from the users location.",
         ["celsius", "fahrenheit"])]
):
    """
    Get the current weather in a given location
    """
    pass  # the function is only a stub, so no implementation needed.

def extract_location(content):
    response = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": content}],
        tools=[
            {
                "type": "function",
                "function": tool.get_tools_spec(get_current_weather),
            }
        ],
        max_tokens=50,
    )
    return response.choices[0].message.tool_calls[0].function.arguments
Let’s run it and try some examples:

print(extract_location("What's the weather like in Paris?"))
print(extract_location("I am going to Munich tomorrow."))
print(extract_location("My friend is in New York. Is it raining there?"))

#### Output ####
# {"location": "Paris, France", "unit": "celsius"}
# {"location": "Munich, Germany", "unit": "celsius"}
# {"location": "New York, NY", "unit": "fahrenheit"}
Voila! Problem solved. Here are some more examples you can try out.

Example 2: Sentiment Analysis
We can use LLMs to help categorize a user’s review into several predefined categories. In this example, we also write a custom system prompt:

from typing import Annotated
from leptonai.util import tool

def respond_to_comment(
    category: Annotated[str,
        ("The sentiment of the user comment.",
         ["positive", "negative", "neutral"])]
):
    pass  # the function is only a stub, so no implementation needed.

def classify_comment(content):
    response = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "system",
                "content": "You are a helpful assistant that helps review customer comments and classify them into categories.",
            },
            {
                "role": "user",
                "content": "customer comment: " + content,
            }
        ],
        tools=[
            {
                "type": "function",
                "function": tool.get_tools_spec(respond_to_comment),
            }
        ],
        max_tokens=20,
    )

    return response.choices[0].message.tool_calls[0].function.arguments

print(classify_comment("pretty useful, solved my problem"))
print(classify_comment("Waste of money"))
print(classify_comment("Okayish considering the price"))

#### Output ####
# {"category": "positive"}
# {"category": "negative"}
# {"category": "neutral"}
Example 3: Comment Community Safety Check
A slightly different example is finding whether the review violates the community safety check (with `boolean` type). This time we will choose to not use the utility function, and pass in a manually written, OpenAI-compatible function json:

def is_safe(content):
    response = client.chat.completions.create(
        model=model,
        messages=[
            {
                "role": "system",
                "content": "You are a helpful assistant that helps decide whether the customer comment violates the community guidelines. Reviews should not be offensive, abusive, or contain any personal information.",
            },
            {
                "role": "user",
                "content": "customer comment: " + content,
            }
        ],
        tools=[
            {
                "type": "function",
                "function": {
                    "name": "respond_to_comment",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "is_safe": {
                                "type": "boolean",
                            },
                        },
                        "required": ["is_safe"],
                    },
                },
            }
        ],
        max_tokens=20,
    )

    return response.choices[0].message.tool_calls[0].function.arguments

print(is_safe("pretty useful, solved my problem"))
print(is_safe("this is the stupiest thing i have ever seen"))
print(is_safe("does not work, broken in two days"))

#### Output ####
# {"is_safe": true}
# {"is_safe": false}
# {"is_safe": true}
Building an end-to-end financial bot
In this last example, we are going to go wild and let LLM help decide whether to buy a stock based on the latest financial information, from the financial modeling prep API.

DISCLAIMER: This is only an illustrative example; Lepton AI is not and does not intend to give any financial advice.

In short, we will ask the LLM to extract information for the following function stub:

from typing import Annotated
from leptonai.util import tool

def get_investment_analysis(
    investment_score: Annotated[
        float,
        "This is a score between 1 to 100, 1 being low and 100"
        " being high, indicating estimated probability of profit if"
        " user buys the stock today and sells in a week"
    ],
    investment_analysis: Annotated[
        str,
        "Detailed stock analysis, including investment recommendation"
    ]
):
    """
    Gets the investment analysis.
    """
    pass
For the full set of code, you can check it out here: https://gist.github.com/Yangqing/eca1bae506e7eccf54b0f3a83411779a

Let’s call it and get some advice:

result = analyze("TSLA")
print("Score: ", result["investment_score"])
print("Analysis: ", result["investment_analysis"])

#### Output ####
Score:  80.0
Analysis:  Tesla's stock has had a remarkable year in 2023, with a 100% increase in price, despite facing challenges. The company is a leader in EV sales and charging infrastructure, making it a strong competitor in the market. However, there are concerns about CEO Elon Musk's many responsibilities and controversial remarks, which may impact the company's performance in 2024. Despite these challenges, analysts remain optimistic about Tesla's earnings growth potential. The stock's 7-day simple moving average (SMA), exponential moving average (EMA), and weighted moving average (WMA) are all trending upwards, indicating a positive outlook. The stock's current price is near a buy point, making it a recommended investment.
These are just a few examples. The structured decoding (function calling) feature is immediately available in all models that Lepton LLM supports, including popular base models like Llama2, CodeLlama, Mixtral, Starcoder, Falcon, Qwen, Yi, Baichuan, and their fine tuned variants. Our API format is compatible with OpenAI, providing our users wtih seamless transitions between ChatGPT and open-source LLM API services.

Conclusions
The shift from conversational interfaces to actionable, structured data output represents a significant leap in the applicability of LLMs, opening up new possibilities for automation and efficiency in various sectors. With structured outputs, LLMs will not only be able to interact with humans in a conversational pattern but will also be able to interface with the vast amount of existing IT and cloud systems programmatically, allowing large-scale applications such as intelligent agents and automation.

The potential of structural decoding goes beyond function calling. With structural decoding, it’s guaranteed that the generated output follows the predefined structure while utilizing the LLMs’ inference capabilities. In fact, Function Calling (JSON formatted output) is only one of many possible structures one can enforce. For example, one can imagine a wider range of structures implemented to deal with multi-modal inputs — image pixels, audio waves, and other specific patterns as long as they can be represented in a finite state machine fashion.

Lepton AI strives to enable creators and enterprises to build AI applications the simple way. Beyond being one of the fastest LLM inference engines today, we are excited to enable structural decoding/function calling for all LLMs hosted on Lepton, bringing open LLMs to the next level of agent applications. We are committed to collaborating with open-source communities to unlock more potential. Happy function calling, and stay tuned for more exciting news!