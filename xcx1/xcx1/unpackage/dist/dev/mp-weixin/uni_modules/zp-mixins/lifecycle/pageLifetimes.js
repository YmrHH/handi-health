"use strict";
function handlePageLifetime(node, lifeName) {
  node.$children.map((child) => {
    if (typeof child[lifeName] == "function")
      child[lifeName]();
    handlePageLifetime(child, lifeName);
  });
}
const pageLifetimes = {
  onLoad() {
  },
  onShow() {
    handlePageLifetime(this, "handlePageShow");
  },
  onHide() {
    handlePageLifetime(this, "handlePageHide");
  },
  onResize() {
    handlePageLifetime(this, "handlePageResize");
  }
};
exports.pageLifetimes = pageLifetimes;
