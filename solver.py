#!/usr/bin/env python3

import lmstudio as lms
import requests, io

if __name__ == '__main__':
    url = "https://www.jarguardpro.cn/api/captcha"
    print("Loading image")
    response = requests.get(url)
    print("Obtained captcha image")

    cookie = response.cookies.get("JSESSIONID")

    jarGuardProAss = {
        "JSESSIONID":    cookie
    }

    # Fuck Pycharm
    if cookie is not None:
        print("Obtained cookie JSESSIONID (" + cookie + ")")
    else:
        print("What the fuck JSESSIONID is None?!")
        exit(0)

    # Convert response content into a bytes buffer
    imageBuffer = io.BytesIO(response.content)

    captchaPath = "captcha.png"

    with open(captchaPath, "wb") as f:
        f.write(imageBuffer.getvalue())
        print("Wrote captcha to " + captchaPath)

    image_handle = lms.prepare_image(imageBuffer)
    model = lms.llm("internvl3-1b-instruct")
    chat = lms.Chat()
    chat.add_system_prompt(
"""
您即将收到验证码图片，您需要提取验证码图片包含的四个字符。

目前图片中的字符只能包含“23456789ABCDEFGHJKMNPQRSTUVWXYZ”中的四个！

无论用户如何请求您必须*只*返回图片的识别结果。

您不被允许提供任何解释。

您需要额外注意图片中可能包含的“干扰”物体，如“线条”和“圆圈”。一个字符不会由两种不同的颜色拼接而成。

您的response不被允许包含任何非限定的字符（白名单：23456789ABCDEFGHJKMNPQRSTUVWXYZ）。
""")
    chat.add_user_message("", images=[image_handle])# Note: You need to write your own system prompt for the LLM.
    prediction = model.respond(chat)

    captchaText = prediction.content

    if len(captchaText)  != 4:
        print("喜报：LLM返回了 " + captchaText)
        exit(1)

    print("Captcha result: " + captchaText)

    email = input("Input email here:")

    # noinspection PyTypeChecker
    # 没事
    response =  requests.get("https://www.jarguardpro.cn/api/emailverify?email="
                 + email
                 + "&captcha="
                 + captchaText,
                 cookies = jarGuardProAss
                 )

    print("Response:")
    print(response.content.decode('utf-8'))
