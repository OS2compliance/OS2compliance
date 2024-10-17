const incidentViewService = new IncidentViewService();
document.addEventListener("DOMContentLoaded", function(event) {
    incidentViewService.init();
});

// Requires incident-service also
function IncidentViewService() {

    this.init = () => {}

    this.setEditable = (dialogId, editable) => {
        const editDescBtn = document.querySelector('#editDescBtn');
        const saveBtn = document.querySelector('#saveBtn');
        const cancelBtn = document.querySelector('#cancelBtn');
        editDescBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = editable ? 'display: block' : 'display: none';
        cancelBtn.style = editable ? 'display: block' : 'display: none';

        let dialog = document.getElementById(dialogId);
        let formElements = dialog.querySelectorAll('select, input, textarea, .dateBtn');
        formElements.forEach(select => {
            select.disabled = !editable;
            let choice = select.choices;
            if (choice !== null && choice !== undefined) {
                editable ? choice.enable() : choice.disable();
            }
        });
    }

}
