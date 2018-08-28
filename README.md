# figma->hiccup

Converts Figma designs into Hiccup and Rum/React components to render in the browser.

## How to use
- See commented code in core namespace
- Replace `'Personal-Access-Tokens` and `'Document-ID` with your own. Read more here https://www.figma.com/developers/docs#auth

## Code overview
- `figma.specs` — specs covering Figma file structure as specified in https://www.figma.com/developers/docs#fileformat
- `figma.hiccup` — compiles Figma file structure into Hiccup

## FAQ

### Why it doesn't layout elements in browser in the same way as in Figma editor?

Because Figma is using its own constraint layout system to position elements on the screen, which is different from CSS. However this behaviour can be reproduced via custom layout algorithm. See http://overconstrained.io/ for reference
