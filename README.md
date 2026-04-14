# 警告：聂凌平有很高的概率在监控本项目！

# Deobfuscation transformers are vibe coded since I don't wanna wast my time on writing deobfuscation transformers an AI slop obfuscator.

<img src="./readme_files/sb.png" alt="聂凌平手动封禁我的IP一小时,操你妈">

<img src="./readme_files/deadass.png" alt="API Captcha 502, rest in piss deadass.">

# JarGuardPro 邮箱验证码自动化发送思路

[获取captcha\(返回图片\)](./readme_files/captcha.http)

验证码图片

<img src="./readme_files/captcha.2AM4.png" alt="captcha image">

[发送邮箱验证码](./readme_files/emailverify.http)

成功返回

```json
{
  "code": 200,
  "message": "邮件发送成功,请查看邮箱",
  "data": null
}
```

验证码错误返回

```json
{
  "code": 201,
  "message": "图形验证码不正确",
  "data": null
}
```

发送频率过高

```json
{
  "code": 600,
  "message": "请等待60秒后再发送验证码",
  "data": null
}
```

被聂凌平自动IPBan

```json
{
  "code": 429,
  "message": "您的请求过于频繁,请1小时后再试",
  "data": null
}
```

```json
{
  "code": 429,
  "message": "您的网络环境异常,已被限制访问,请1小时后再试",
  "data": null
}
```

被聂凌平自手动IPBan:

```text
401(Nothing else)
```

注：一个验证码可以多次使用,也就是你*短时间内*emailverify email admin@gov.cn captcha cfcy,把email换成admin@google.com也行。
注：目前你需要带好JSESSIONID cookie
注：老傻子聂凌平不检测请求头,`User-Agent`不包含`Mozilla`都不自动风控
注：老傻子聂凌平封IP不封JSESSIONID
~~注：碰到Rate limiter了你把delay拉大点就随便了~~
注：现在Rate limiter增强了所以你需要代理池来短时间内spam一个人的邮箱

Solving the captcha(Extract 4 characters from image):

Use `InternVL3-1B-Instruct-GGUF` or `glm-ocr`.

# Shame

<div align="center">
<img src="./readme_files/dot.png" alt="Leaving shit characters inside index.html">

<img src="./readme_files/WhereIsMyCaptchaImage.png" alt="Where is my fucking captcha image at Login frame?">

<img src="./readme_files/CopyPasting.png" alt= "Same text redirects to different links.">

<img src="./readme_files/AcceptTerms1.png" alt = "That's ok...">
<img src="./readme_files/AcceptTerms2.png" alt = "But.. WTF?">
<img src="./readme_files/recover404.png" alt = "He is trying to copy-paste AI slop but forgot to fix links, resulting users cannot recover accounts normally.">
</div>