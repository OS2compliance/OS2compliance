ClassicEditor.create(document.querySelector( '#description-editor' ), {
    licenseKey: ''
}).then( editor => {
        window.editor = editor;
        setEditState(false)
    }).catch( error => {
        console.error( error );
    });

function setEditState(editable) {
    const editDesc = document.querySelector('#editDescId');
    const readDesc = document.querySelector('#readDescId');
    const status = document.querySelector('#status');
    readDesc.style = editable ? 'display: none' : 'display: block';
    editDesc.style = editable ? 'display: block !important' : 'display: none !important';
    status.disabled = !editable;

    document.querySelectorAll('.edit-input').forEach(elem => {
        elem.readOnly = !editable;
    });

    const editDescBtn = document.querySelector('#editDescBtn');
    const saveBtn = document.querySelector('#saveBtn');
    const cancelBtn = document.querySelector('#cancelBtn');
    editDescBtn.style = editable ? 'display: none' : 'display: block';
    saveBtn.style = editable ? 'display: block' : 'display: none';
    cancelBtn.style = editable ? 'display: block' : 'display: none';
}
