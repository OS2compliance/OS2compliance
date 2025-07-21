

export default class KLESelectionService {
    mainGroupSelectId = 'mainGroupSelector'
    groupSelectId = 'groupSelector'
    mainGroupSelectorInstance = null
    groupSelectorInstance = null

    constructor() {
    }

    initKLEMainGroupSelect() {
        if (this.mainGroupSelectorInstance) {
            this.mainGroupSelectorInstance.destroy()
        }

        const mainGroupSelect = document.getElementById(this.mainGroupSelectId);
        this.mainGroupSelectorInstance = initSelect(mainGroupSelect);

        mainGroupSelect.addEventListener('change', async (e) => {
            await this.#getGroupOptionsFragment()
        })

        this.#initGroupSelect()

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
        this.#initGroupSelect()
    }

    #initGroupSelect() {
        const groupSelect = document.getElementById(this.groupSelectId)
        this.groupSelectorInstance = initSelect(groupSelect);
    }
}