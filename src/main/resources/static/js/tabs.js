
/*
 *Makes a tab and it's content inactive
 */
function makeInactive(items) {
    const content = Object.keys(items).map((item) => {
        items[item].classList.remove("active");
        items[item].classList.remove("show");
    });
}

/*
 *Display the selected tab.
 */
function activateTab(e) {
    //refers to the element whose event listener triggered the event
    const clickedTab = e.currentTarget;
    clickedTab.classList.add("active");
}

/*
 * Display the selected tab content.
 */
function activateTabContent(e) {
    // gets the element on which the event originally occurred
    const anchorReference = e.target;
    const activePaneID = anchorReference.getAttribute("data-bs-target");
    const activePane = document.querySelector(activePaneID);
    activePane.classList.add("active");
    activePane.classList.add("show");
}
