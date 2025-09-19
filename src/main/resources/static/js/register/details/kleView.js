

export default class KLEView {
    kleTabBodyElement

    init() {
        this.kleTabBodyElement = document.getElementById('_dm-tabsKLEDetails');
        this.initInstructionTextToggle()
    }

    initInstructionTextToggle() {
        this.kleTabBodyElement.addEventListener('click', (e) => {
            if (e.target?.classList.contains('keywordToggle')) {
                // Handle keyword toggle
                const detailElementId = e.target.dataset.keywordId
                this.handleAdditionalInfoButtonToggle(e, detailElementId)
            } else if (e.target?.classList.contains('instructionTextToggle')) {
                // Handle instructiontext toggle
                const instructionTextId = e.target.dataset.instructionTextId
                this.handleAdditionalInfoButtonToggle(e, instructionTextId)
            } else if (this.hasClassRecursively(e.target, 'accordion-header',this.kleTabBodyElement)) {
                // Handle accordion collapse
                this.toggleCollapseOfParent(e.target);
            }
        })
    }

    handleAdditionalInfoButtonToggle(e, detailElementId) {
        e.preventDefault();
        e.target.classList.toggle('active');
        if (detailElementId) {
            const detailElement = document.getElementById(detailElementId)
            if (detailElement) {
                detailElement.hidden = !detailElement.hidden
                if (!detailElement.hidden) {
                    this.toggleCollapseOfParent(e.target, true);
                }
            }
        }
    }

    toggleCollapseOfParent(element, open){
        const parent = element.closest('.accordion-item')
        const body = parent.querySelector('.accordion-collapse')
        if (open) {
            body.classList.remove('collapse');
        } else if (open === false) {
            body.classList.add('collapse');
        } else {
            body.classList.toggle('collapse');
        }
    }

    /**
     * Checks if the provided element or one of its ancestors has the specified class. Stops at the provided endpointElement
     * @param element element to check
     * @param className class to search for
     * @param endpointElement Element to stop at if no match is found
     * @returns {boolean|boolean|*}
     */
    hasClassRecursively(element, className, endpointElement) {
        if (!element || !className) {
            return false;
        }
        if (element.classList.contains(className)) {
            return true;
        }
        if (endpointElement === element) {
            return false
        }
        return this.hasClassRecursively(element.parentElement, className, endpointElement)
    }
}
