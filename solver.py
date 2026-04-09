#!/usr/bin/env python3

import lmstudio as lms
import requests, io
import sys

if __name__ == '__main__':
    modelName = "internvl3-1b-instruct"
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

    # captchaPath = "captcha.png"

    # with open(captchaPath, "wb") as f:
    #     f.write(imageBuffer.getvalue())
    #     print("Wrote captcha to " + captchaPath)

    image_handle = lms.prepare_image(imageBuffer)
    model = lms.llm(modelName)
    chat = lms.Chat()
    chat.add_system_prompt(
"""
您即将看到一张图片，您需要从左到右定位图片中4个字符的位置,逐个字符识别（每次只识别一个）,最终拼接为4个字符!
可能出现的字符包含'23456789ABCDEFGHJKMNPQRSTUVWXYZ'，但是有一些你需要注意的。
比如说，字母Q不会以小写的形式出现，字母X可能会像一个非ASCII字符，图片中还可能有一些干扰物。
下面是你的自检规则，你必须重点注意：
- 是否正好4个字符？
- 是否全部属于给定集合？
- 是否全部为ASCII字符？
- 是否包含空格？

如果不满足，请重新识别。

最终输出要求（非常重要）：
- 只输出最终验证码
- 不要解释
- 不要空格
- 不要换行
""")
    chat.add_user_message("请严格按照系统指令识别验证码，并只输出最终结果。", images=[image_handle])# Note: You need to write your own system prompt for the LLM.
    prediction = model.respond(chat)

    captchaText = prediction.content

    if len(captchaText)  != 4:
        print("喜报：" + modelName + "返回了 " + captchaText)
        exit(1)

    print("Captcha result: " + captchaText)

    # email = input("Input email here:")
    email = sys.argv[1]

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
