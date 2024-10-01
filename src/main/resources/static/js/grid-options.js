
const gridOptions = new GridOptions();
function GridOptions() {
    this.grid = null;
    this.init = function (grid) {
        this.grid = grid;
    }

    this.populateDropDown = function (ulElement) {
        return this.grid.config.columns.forEach((column) => {
            if (column.name !== "id") {
                const li = document.createElement("li");
                const content = document.createTextNode(column.name);
                li.classList.add("dropdown-item");
                li.setAttribute("onclick", "gridOptions.toggleVisibility(this)");
                li.setAttribute("onclick", "gridOptions.toggleVisibility(this)");
                li.dataset.column
                li.appendChild(content);
                ulElement.appendChild(li);
            }
        })
    }

    this.toggleVisibility = function (element) {
        this.grid.config.columns.forEach((column) => {
            //
        });
    }

}
