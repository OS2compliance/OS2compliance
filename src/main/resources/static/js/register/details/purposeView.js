/**
 * Functionality related to the "Formål og lovhjemmel" page of the register detail view
 */
export default class RegisterPurposeService {
    #consentTriggeringIdentifiers = ['register-gdpr-p6-a', 'register-gdpr-p7-a']
    #supplementalLegalTriggeringIdentifiers = ['register-gdpr-p6-c', 'register-gdpr-p6-e', 'register-gdpr-p7-c', 'register-gdpr-p7-f']
    #consentTriggeringElements = new Map()
    #supplementalLegalElements = new Map()

    init() {
        this.initGDPRCheckboxChange()

        // Check for conditional fields visibility
        this.#consentFieldChange()
        this.#supplementalLegalFieldChange()
    };

    setPurposeEditState(editable) {
        const editBtn = document.querySelector('#editPurposeBtn');
        const cancelBtn = document.querySelector('#cancelPurposeBtn');
        const saveBtn = document.querySelector('#savePurposeBtn');
        const purpose = document.querySelector('#purpose');
        const purposeNotes = document.querySelector('#purposeNotes');
        const informationObligationDesc = document.querySelector('#informationObligationDesc');
        const informationObligation = document.querySelector('#informationObligation');
        const checkboxes = document.getElementsByClassName("form-check-input");
        const consent = document.querySelector('#consent');
        const suplementalField = document.getElementById('supplementalLegalBasis')

        editBtn.style = editable ? 'display: none' : 'display: block';
        saveBtn.style = !editable ? 'display: none' : 'display: block';
        cancelBtn.style = !editable ? 'display: none' : 'display: block';

        for (const element of checkboxes) {
            element.disabled = !editable;
        }
        informationObligationDesc.readOnly = !editable;
        consent.readOnly = !editable;
        informationObligation.disabled = !editable;
        purpose.readOnly = !editable;
        purposeNotes.readOnly = !editable;
        suplementalField.readOnly = !editable;
        if (!editable) {
            const form = document.querySelector('#editPurposeId');
            form.reset();
        }
    }

    initGDPRCheckboxChange() {
        for (const identifier of this.#consentTriggeringIdentifiers) {
            const element = document.getElementById(identifier)
            this.#consentTriggeringElements.set(identifier, element);
            element?.addEventListener('change', (e) => this.#onGDPRCheckboxChange(element))

        }
        for (const identifier of this.#supplementalLegalTriggeringIdentifiers) {
            const element = document.getElementById(identifier)
            this.#supplementalLegalElements.set(identifier, element);
            element?.addEventListener('change', (e) => this.#onGDPRCheckboxChange(element))
        }
    }


    #onGDPRCheckboxChange(element) {
        if (this.#consentTriggeringIdentifiers.includes(element.id)) {
            this.#consentFieldChange()
        }
        if (this.#supplementalLegalTriggeringIdentifiers.includes(element.id)) {
            this.#supplementalLegalFieldChange()
        }
    }

    #consentFieldChange() {
        // If either of these are true, show the field
        const anyTrue = this.#consentTriggeringElements.get('register-gdpr-p6-a')?.checked || this.#consentTriggeringElements.get('register-gdpr-p7-a')?.checked

        const consentField = document.getElementById('consentGroup')
        consentField.hidden = !anyTrue;


    }

    #supplementalLegalFieldChange() {
        // If either (6-c AND 6-e) is true OR (9-c AND 9-f) is true, show the field
        const art6IsTrue = this.#supplementalLegalElements.get('register-gdpr-p6-c')?.checked && this.#supplementalLegalElements.get('register-gdpr-p6-e')?.checked
        const art9IsTrue = this.#supplementalLegalElements.get('register-gdpr-p7-c')?.checked && this.#supplementalLegalElements.get('register-gdpr-p7-f')?.checked

        const suplementalField = document.getElementById('supplementalLegalGroup')

        suplementalField.hidden = !(art6IsTrue || art9IsTrue)

    }

}
