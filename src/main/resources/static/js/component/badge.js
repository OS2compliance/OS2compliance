export class BadgeData {
    content
    href
    colorClass = 'bg-info'
    constructor(content, href = null, colorClass = 'bg-info'){
        this.content = content
        this.href = href
        this.colorClass = colorClass
    }
}

export function createBadges(badgeData) {
    const container = document.createElement('div')
    container.className = 'd-flex flex-wrap'

    if (!badgeData || !Array.isArray(badgeData) || badgeData.length < 1) {
        return container
    }

    badgeData.forEach(d => {
        console.log(d)
        const badge = createBadge(d.content, d.href, d.colorClass);
        if (badge) {
            container.appendChild(badge);
        } else {
            console.warn("Attempted to create badge with no content");
        }
    });

    return container
}

function createBadge(content, href = null, colorClass = 'bg-info') {
    if (!content) return null;

    const span = document.createElement('span');
    span.className = `badge list-badge me-1 mb-1 ${colorClass}`;

    if (href && typeof href === 'string') {
        span.appendChild(createLinkForBadge(content, href));
    } else {
        span.textContent = content;
    }

    return span;
}

function createLinkForBadge(content, href) {
    const anchor = document.createElement('a')
    anchor.href = href
    anchor.textContent = content
    return anchor
}