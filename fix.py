with open('app/src/main/java/com/example/stripeseburoterminal/terminal/TerminalViewModel.kt', 'r') as f:
    lines = f.readlines()
with open('app/src/main/java/com/example/stripeseburoterminal/terminal/TerminalViewModel.kt', 'w') as f:
    f.writelines(lines[:486])
