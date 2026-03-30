export default class KLESelectionService {
    mainGroupSelectId = 'mainGroupSelector'
    groupSelectId = 'groupSelector'
    subjectSelectId = 'secondaryGroupSelector'
    legalReferenceSelectId = 'relevantLegalReferencesSelector'
    mainGroupSelectorInstance = null
    groupSelectorInstance = null
    subjectSelectorInstance = null
    legalReferenceSelectorInstance = null

    constructor() {
    }

    initKLEMainGroupSelect() {
        if (this.mainGroupSelectorInstance) {
            this.mainGroupSelectorInstance.destroy()
        }

        const mainGroupSelect = document.getElementById(this.mainGroupSelectId);
        this.mainGroupSelectorInstance = initSelect(mainGroupSelect, 'form-control', {searchChoices: true});

        mainGroupSelect.addEventListener('change', async (e) => {
            await this.#getGroupOptionsFragment()
        })
    }

    async #getGroupOptionsFragment() {
        if (this.groupSelectorInstance) {
            this.groupSelectorInstance.destroy()
        }

        // Get selected maingroups and selected groups
        const mainGroupSelector = document.getElementById(this.mainGroupSelectId);
        const selectedMainGroups = [...mainGroupSelector.selectedOptions].map(o => o.value);
        const groupSelector = document.getElementById(this.groupSelectId);
        const selectedGroups = [...groupSelector.selectedOptions].map(o => o.value);

        // fetch groupSelectOptions
        const url = `/kle/maingroup/groups?mainGroupNumbers=${selectedMainGroups}&selectedGroups=${selectedGroups}`;
        await fetchHtml(url, this.groupSelectId);

        // Re-initialize Choices.js after HTML has been loaded
        this.initGroupSelect();
    }

    initGroupSelect() {
        const groupSelect = document.getElementById(this.groupSelectId)
        this.groupSelectorInstance = initSelect(groupSelect, 'form-control', {searchChoices: true});
    }

    initSubjectSelect() {
        const subjectSelect = document.getElementById(this.subjectSelectId)
        this.subjectSelectorInstance = initSelect(subjectSelect, 'form-control', {searchChoices: true});
    }

    initLegalReferenceSelect() {
        if (this.legalReferenceSelectorInstance) {
            this.legalReferenceSelectorInstance.destroy()
        }

        const legalRefSelect = document.getElementById(this.legalReferenceSelectId);
        this.legalReferenceSelectorInstance = initSelect(legalRefSelect, 'form-control', {searchChoices: true});
    }
}