export default class OnUnSubmittedService {
    #unsubmittedChange = false;

    constructor() {
        window.addEventListener("beforeunload", (event) => {
            if(this.#unsubmittedChange) {
                event.preventDefault();
            }
        });
    }

    setChangesMade() {
        this.#unsubmittedChange = true;
    }

    reset() {
        this.#unsubmittedChange = false;
    }
};