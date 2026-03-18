export class BadgeData {
    content
    href
    title
    hexCode
    additionalClass

    constructor(content, href = null, title = "", hexCode = null, additionalClass = "") {
        this.content = content
        this.href = href
        this.title = title
        this.hexCode = hexCode
        this.additionalClass = additionalClass
    }
}

export function createBadges(badgeData) {
    const container = document.createElement('div')
    container.className = 'd-flex flex-wrap'

    if (!badgeData || !Array.isArray(badgeData) || badgeData.length < 1) {
        return container
    }

    badgeData.forEach(d => {
        const badge = createBadge(d.content, d.href, d.title, d.hexCode, d.additionalClass);
        if (badge) {
            container.appendChild(badge);
        } else {
            console.warn("Attempted to create badge with no content");
        }
    });

    return container
}

function createBadge(content, href = null, title = "", hexCode = null, additionalClass = "") {
    if (!content) return null;

    const span = document.createElement('span');
    span.className = `badge list-badge me-1 mb-1 ${additionalClass}`;


    if (href && typeof href === 'string') {
        span.appendChild(createLinkForBadge(content, href));
    } else {
        span.textContent = content;
    }

    // background color
    if (hexCode) {
        span.style.background = hexCode
    }

    // title for mouseover text
    if (title) {
        span.title = title
    }

    return span;
}

function createLinkForBadge(content, href) {
    const anchor = document.createElement('a')
    anchor.href = href
    anchor.textContent = content
    return anchor
}