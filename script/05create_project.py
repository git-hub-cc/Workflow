import os
import re

def create_project_from_ai_output(input_file="01code.md"):
    """
    Parses a markdown file containing multiple code blocks and creates the
    full project structure with files.

    This script is specifically tailored for the format:
    --- START OF FILE path/to/your/file.ext ---
    ```language
    ... file content ...
    --- END OF FILE path/to/your/file.ext ---

    Args:
        input_file (str): The name of the markdown file to read from.
    """
    print(f"--- Starting Project Generator ---")
    print(f"Reading from input file: '{input_file}'")

    try:
        with open(input_file, 'r', encoding='utf-8') as f:
            content = f.read()
    except FileNotFoundError:
        print(f"\n[ERROR] Input file '{input_file}' not found.")
        print("Please ensure the AI's output is saved in this file in the same directory as the script.")
        return
    except Exception as e:
        print(f"\n[ERROR] Could not read file '{input_file}': {e}")
        return

    # --- CORRECTED REGEX ---
    # This regex correctly identifies that the code block is terminated by the
    # "--- END OF FILE ---" marker, NOT by a closing ``` fence.
    #
    # Breakdown:
    #   - `--- START OF FILE\s+(.+?)\s+---`: Matches the start marker and captures (Group 1) the file path.
    #   - `\s*```[a-zA-Z]*\n`: Matches the opening code fence and the language identifier (e.g., ```vue\n).
    #   - `(.*?)`: The key part. Non-greedily captures (Group 2) all content until the next part of the pattern is found.
    #   - `(?=\s*--- END OF FILE)`: This is a positive lookahead. It ensures that the capture stops right before
    #     the "--- END OF FILE" marker without including it in the content. This is the fix.
    pattern = re.compile(
        r"--- START OF FILE\s+(.+?)\s+---\s*"
        r"```[a-zA-Z]*\n"
        r"(.*?)"
        r"(?=\s*--- END OF FILE)",
        re.DOTALL
    )

    matches = pattern.findall(content)

    if not matches:
        print("\n[WARNING] No file blocks were found, even with the corrected pattern.")
        print("Please check for any subtle formatting issues in '01code.md'.")
        return

    print(f"\nFound {len(matches)} files to create. Starting project generation...\n")

    created_files_count = 0
    failed_files = []

    for file_path, code_content in matches:
        # Sanitize and normalize the path for the current OS
        file_path = file_path.strip()
        normalized_path = os.path.normpath(file_path)

        if not normalized_path:
            continue

        print(f"-> Processing: {normalized_path}")

        try:
            directory = os.path.dirname(normalized_path)
            if directory:
                os.makedirs(directory, exist_ok=True)

            # Write the file content, stripping any trailing whitespace before the end marker
            with open(normalized_path, 'w', encoding='utf-8', newline='\n') as f:
                f.write(code_content.rstrip())

            created_files_count += 1

        except Exception as e:
            error_message = f"   [ERROR] Failed to create file {normalized_path}: {e}"
            print(error_message)
            failed_files.append((normalized_path, str(e)))

    print("\n--- Generation Report ---")
    print(f"✅ Successfully created {created_files_count} files.")

    if failed_files:
        print(f"❌ Failed to create {len(failed_files)} files:")
        for path, reason in failed_files:
            print(f"  - {path}\n    Reason: {reason}")
    else:
        print("All files generated without errors.")
    print("\nProject generation complete.")

if __name__ == "__main__":
    create_project_from_ai_output()