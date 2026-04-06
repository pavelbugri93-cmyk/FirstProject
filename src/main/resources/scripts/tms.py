import asyncio, json, re
from playwright.async_api import async_playwright

SEARCH_LIST = [
    "7500F", "RTX 4060", "RTX 4060 TI", "RTX 5060", "RTX 5060 Ti",
    "RTX 4070", "RTX 4070 Super", "RTX 5070", "RTX 5070 TI",
    "RX 7600", "RX 7600XT", "RX 7700 XT", "RX 9060 XT", "RX 9070 XT"
]

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        results = []

        for item in SEARCH_LIST:
            url = f"https://tms.co.il/index.php?route=product/search&search={item}&sort=p.price&order=ASC"
            try:
                await page.goto(url, timeout=10000)
                card = page.locator(".product-card").first

                if await card.count() == 0:
                    print(f"[-] {item} not found")
                    results.append({
                        "product": item,
                        "price": 0,
                        "link": f"https://tms.co.il/index.php?route=product/search&search={item}"
                    })
                    continue

                price_text = await card.locator(".product-card__price-normal").first.inner_text()
                numbers = re.findall(r'\d+', price_text.replace(',', ''))

                if not numbers:
                    continue

                clean_price = int(numbers[0])
                link = await card.locator("a").first.get_attribute("href")

                results.append({
                    "product": item,
                    "price": clean_price,
                    "link": link
                })
                print(f"[+] {item}: {clean_price} ILS")

            except Exception as e:
                print(f"[!] Error for {item}: {str(e)}")
                continue

        with open("pc_prices.json", "w", encoding="utf-8") as f:
            json.dump(results, f, ensure_ascii=False, indent=2)

        await browser.close()
        print(f"\n[*] Done! Collected {len(results)} products.")

if __name__ == "__main__":
    asyncio.run(main())