import os
import re

mappings_file = r"C:\Users\hican\.gradle\caches\fabric-loom\1.21.11\net.fabricmc.yarn.1_21_11.1.21.11+build.5-v2\mappings.tiny"
target_dir = r"d:\ModJava\MahikariClient\src\main\java\mahikariui"

class_map = {}
method_map = {}
field_map = {}

with open(mappings_file, 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line: continue
        parts = line.split('\t')
        if parts[0] == 'c':
            if len(parts) >= 4:
                intermediary = parts[2].replace('/', '.')
                named = parts[3].replace('/', '.')
                class_map[intermediary] = named
                
                simple_int = intermediary.split('.')[-1]
                simple_named = named.split('.')[-1]
                class_map[simple_int] = simple_named
        elif parts[0] == 'm':
            if len(parts) >= 5:
                intermediary = parts[3]
                named = parts[4]
                method_map[intermediary] = named
        elif parts[0] == 'f':
            if len(parts) >= 5:
                intermediary = parts[3]
                named = parts[4]
                field_map[intermediary] = named

sorted_class_keys = sorted([k for k in class_map.keys() if 'class_' in k], key=len, reverse=True)
sorted_method_keys = sorted([k for k in method_map.keys() if 'method_' in k], key=len, reverse=True)
sorted_field_keys = sorted([k for k in field_map.keys() if 'field_' in k], key=len, reverse=True)

count = 0
for root, _, files in os.walk(target_dir):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            new_content = content
            
            for k in sorted_class_keys:
                if k in new_content:
                    new_content = re.sub(r'\b' + re.escape(k) + r'\b', class_map[k].replace('$', '.'), new_content)
            
            for k in sorted_method_keys:
                if k in new_content:
                    new_content = re.sub(r'\b' + re.escape(k) + r'\b', method_map[k], new_content)
                    
            for k in sorted_field_keys:
                if k in new_content:
                    new_content = re.sub(r'\b' + re.escape(k) + r'\b', field_map[k], new_content)

            if content != new_content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                count += 1

print(f"Updated {count} files.")
