#!/usr/bin/env python3
import sys
import io
import requests
import lmstudio as lms

def solve_captcha(image_buffer):
    model_name = "glm-ocr"
    image_handle = lms.prepare_image(image_buffer)
    model = lms.llm(model_name)

    chat = lms.Chat()
    chat.add_system_prompt(
        "Identify and output 4 characters from the image. Characters: 23456789ABCDEFGHJKMNPQRSTUVWXYZ. No spaces, no extra text. If the image appears to be broken or youy can't see it, response with 'Fuck!'(That's ok)"
    )
    chat.add_user_message("Captcha image:", images=[image_handle])

    response = model.respond(chat)
    # Sanitise: remove newlines, spaces, and quotes
    return response.content.strip().replace(" ", "").replace('"', '')

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: ./script.py <email>")
        sys.exit(1)

    email = sys.argv[1]
    url_captcha = "https://www.jarguardpro.cn/api/captcha"

    session = requests.Session() # Session automatically handles cookies
    res = None
    try:
        res = session.get(url_captcha)
    except requests.exceptions.SSLError:
        print("L bad proxy")
        exit(1)
    if res.status_code != 200:
        print(f"聂凌平\'s captcha APi returned status code {res.status_code}")
        print(f"Content dump:\n {res.content.decode('utf-8')}")
        exit(1)

    img_io = io.BytesIO(res.content)
    captcha_text = solve_captcha(img_io)

    if len(captcha_text) != 4:
        print(f"Failure: Model returned '{captcha_text}'")
        sys.exit(1)

    print(f"Recognized: {captcha_text}")

    verify_url = f"https://www.jarguardpro.cn/api/emailverify?email={email}&captcha={captcha_text}"
    res_verify = session.get(verify_url)

    if res_verify.status_code == 200:
        print(res_verify.json().get('message'))
    else:
        print(f"Status {res_verify.status_code}: {res_verify.text}")