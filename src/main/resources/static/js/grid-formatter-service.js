/**
 * Used to format the "Actions" column of a grid. Assumes there are editbutton and deletebutton templates in the HTML
 * Will assume the "cell" parameter holds an array of AllowedActions, which determines update and delete permissions for the user
 * Will set the id as an 'identifier' data attribute on the buttons. Will also set the name as a data attribute on the delete button
 * @param cell cell parameter passed from grid formatter
 * @param row row parameter passed from grid formatter
 * @param rowId id of the entity represented by the row. Will be set as data attribute 'identifier' on both buttons
 * @param rowName name of the entity represented by the row. Will be set as data attribute on delete button
 * @returns {*}
 */
function formatAllowedActions(cell, row, rowId, rowName = '') {
    const container = document.createElement('span');

    if (cell?.includes("UPDATE")) {
        const buttonFragment = editTemplate.content.cloneNode(true);
        const button = buttonFragment.firstElementChild;
        button.dataset.identifier = rowId;
        container.appendChild(button);
    }
    if (cell?.includes("DELETE")) {
        const buttonFragment = deleteTemplate.content.cloneNode(true);
        const button = buttonFragment.firstElementChild;
        button.dataset.identifier = rowId;
        button.dataset.name = name;
        container.appendChild(button);
    }

    return gridjs.html(container.innerHTML); // Ugly hack because grid.js sucks
}