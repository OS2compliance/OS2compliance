export default function formatTags(cell, row) {
    if (cell.length < 0) {
        return ''
    }
    const container = createContainer()

    cell.map(tag => {
        let element =createTag(tag.label)
        element = setColor(element, tag.color, tag.contrast)
        container.appendChild(element)
    })

    return gridjs.html(container.outerHTML)

}

function setColor(element, backgroundColor, textColor) {
    element.style.backgroundColor = backgroundColor
    element.style.color = textColor
    return element;
}

function createTag(text) {
    const element = document.createElement('span')
    element.classList.add('tag-badge')
    element.textContent = text
    return element
}

function createContainer() {
    const element = document.createElement('div')
    element.classList.add('d-flex')
    element.classList.add('gap-1')
    return element
}