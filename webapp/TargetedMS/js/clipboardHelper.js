/** Copies a string argument to the clipboard, via a text area temporarily injected into the DOM */
function copyStringToClipboard(s)
{
    var textarea = document.createElement('textarea');
    textarea.setAttribute('type', 'hidden');
    textarea.textContent = s;
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    textarea.remove();
    window.alert('Copied "' + s + '" to the clipboard.');
}