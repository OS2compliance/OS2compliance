
const gridOptions = new GridOptions();
function GridOptions() {
    this.grid = null;
    this.ulElement = null;
    this.gridOptionsKey = `${window.location.pathname}-grid-options`;

    this.init = function (grid, ulElement) {
        this.grid = grid;
        this.ulElement = ulElement;
        this.loadFromLocalStorage();
        this.populateDropDown();
        this.updateLabels();
    }

    this.populateDropDown = function () {
        return this.iterateColumns((column) => {
            const li = document.createElement("li");
            const content = document.createTextNode(column.name);
            const icon = document.createElement("i");
            icon.classList.add("ti-check");
            icon.classList.add("me-1");
            li.classList.add("dropdown-item");
            li.classList.add("cursor-hand");
            li.setAttribute("onclick", "gridOptions.toggleVisibility(this)");
            li.setAttribute("href", "#");

            li.dataset.id = column.id;
            li.appendChild(icon);
            li.appendChild(content);
            this.ulElement.appendChild(li);
        })
    }

    this.updateLabels = function () {
        const lis = this.ulElement.querySelectorAll("li");
        lis.forEach((item) => {
            const columnId = item.dataset.id;
            this.grid.config.columns.forEach((column) => {
                if (column.id === columnId) {
                    let icon = item.querySelector("i");
                    if (column.hidden) {
                        icon.classList.remove("ti-check");
                        icon.classList.add("ti-minus");
                    } else {
                        icon.classList.remove("ti-minus");
                        icon.classList.add("ti-check");
                    }
                }
            });
        });
    }

    this.toggleVisibility = function (element) {
        this.grid.config.columns.forEach((column) => {
            if (column.id === element.dataset.id) {
                column.hidden = !column.hidden;

                //Updates subcolumn headers if custom grid search functionality is used
                if (column.onHiddenUpdate) {
                    column.onHiddenUpdate()
                }
            }
        });
        this.updateLabels();
        this.saveToLocalStorage();
        this.grid.forceRender();
    }

    this.loadFromLocalStorage = function () {
        let savedData = localStorage.getItem(this.gridOptionsKey);
        if (savedData !== null) {
            const object = JSON.parse(savedData);
            this.iterateColumns((column) => {
                if (object[column.id] !== null) {
                    column.hidden = !object[column.id];
                }
                //Updates subcolumn headers if custom grid search functionality is used
                if (column.onHiddenUpdate) {
                    column.onHiddenUpdate()
                                }
            });
            this.grid.forceRender();
        }
    }

    this.saveToLocalStorage = function () {
        const object = {};
        this.iterateColumns((column) => {
            object[column.id] = !column.hidden;
        });
        localStorage.setItem(this.gridOptionsKey, JSON.stringify(object));
    }

    this.iterateColumns = function(callback) {
        return this.grid.config.columns.forEach((column) => {
            if (column.id !== "id" && column.id !== "handlinger" && column.id !== "titel" && column.id !== "navn"
                && column.id !== "kitos" && column.id !== "opgavenavn" && column.id !== "risikovurdering"
                && column.id !== "supplierId" && column.id !== "outstandingTaskId" && column.id !== "fromExternalSource"
                && column.id !== "externalLink") {
                callback(column);
            }
        });
    }

}
