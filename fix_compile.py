import os

def fix_view_model():
    path = 'app/src/main/java/com/example/stripeseburoterminal/terminal/TerminalViewModel.kt'
    with open(path, 'r') as f:
        content = f.read()

    # 1. options.name -> options.toString()
    content = content.replace('options.name', 'options.toString()')
    
    # 2. message.name -> message.toString()
    content = content.replace('message.name', 'message.toString()')
    
    # 3. cancel() -> cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: com.stripe.stripeterminal.external.models.TerminalException) {} })
    cb = 'cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: com.stripe.stripeterminal.external.models.TerminalException) {} })'
    content = content.replace('cancel()', cb)
    
    # 4. currency.uppercase() -> (currency ?: "").uppercase()
    content = content.replace('paymentIntent.currency.uppercase()', '(paymentIntent.currency ?: "").uppercase()')
    
    # 5. confirmedPaymentIntent.id -> (confirmedPaymentIntent.id ?: "")
    content = content.replace('paymentIntentId = confirmedPaymentIntent.id', 'paymentIntentId = confirmedPaymentIntent.id ?: ""')
    content = content.replace('receiptNumber = confirmedPaymentIntent.id', 'receiptNumber = confirmedPaymentIntent.id ?: ""')
    
    # 6. extractCardLastFour
    old_extract = 'return paymentIntent.charges?.firstOrNull()?.paymentMethodDetails?.cardDetails?.last4'
    new_extract = 'return null'
    content = content.replace(old_extract, new_extract)

    with open(path, 'w') as f:
        f.write(content)

def fix_payment_result():
    path = 'app/src/main/java/com/example/stripeseburoterminal/terminal/PaymentResult.kt'
    with open(path, 'r') as f:
        content = f.read()
    content = content.replace('val receiptNumber: String = paymentIntent.id', 'val receiptNumber: String = paymentIntent.id ?: ""')
    with open(path, 'w') as f:
        f.write(content)

fix_view_model()
fix_payment_result()
