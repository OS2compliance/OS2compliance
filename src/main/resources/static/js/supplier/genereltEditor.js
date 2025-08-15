
document.addEventListener("DOMContentLoaded", () => {
    window.CreateCkEditor(document.querySelector('#description-editor'), editor => {
        window.editor = editor;
        setEditState(false);
    });
});

function setEditState(editable) {
    const editDesc = document.querySelector('#editDescId');
    const readDesc = document.querySelector('#readDescId');
    const status = document.querySelector('#status');
    if (editDesc) {
        readDesc.style = editable ? 'display: none' : 'display: block';
    }
    if (editDesc) {
        editDesc.style = editable ? 'display: block !important' : 'display: none !important';
    }
    if (status) {
        status.disabled = !editable;
    }

    document.querySelectorAll('.edit-input').forEach(elem => {
        elem.readOnly = !editable;
    });

    const editDescBtn = document.querySelector('#editDescBtn');
    const saveBtn = document.querySelector('#saveBtn');
    const cancelBtn = document.querySelector('#cancelBtn');
    if (editDescBtn) {
        editDescBtn.style = editable ? 'display: none' : 'display: block';
    }
    if (saveBtn) {
        saveBtn.style = editable ? 'display: block' : 'display: none';
    }
    if (cancelBtn) {
        cancelBtn.style = editable ? 'display: block' : 'display: none';
    }
}
