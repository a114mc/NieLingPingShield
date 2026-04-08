#!/usr/bin/env python3

import lmstudio as lms
import requests, io

if __name__ == '__main__':
    url = "https://www.jarguardpro.cn/api/captcha"
    response = requests.get(url)

    jarGuardProAss = {
        "JSESSIONID":    response.cookies.get("JSESSIONID")
    }

    # Convert response content into a bytes buffer
    image_buffer = io.BytesIO(response.content)

    with open("captcha.png", "wb") as f:
        f.write(image_buffer.getvalue())

    image_handle = lms.prepare_image(image_buffer)
    model = lms.llm("internvl3-1b-instruct")
    chat = lms.Chat()
    chat.add_user_message("", images=[image_handle])# Note: You need to write your own system prompt for the LLM.
    prediction = model.respond(chat)

    captchaText = prediction.content

    if len(captchaText)  != 4:
        print("喜报：LLM返回了 " + captchaText)
        exit(1)

    print("Captcha result: "+captchaText)

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
