/**
 * 页面重定向切换动态效果
 * 参考MVCHelper的状态管理和动画过渡思想
 */

class PageTransition {
  constructor() {
    this.isTransitioning = false;
    this.transitionDuration = 300; // 过渡动画持续时间（毫秒）
    this.init();
  }

  init() {
    // 监听所有链接的点击事件
    document.addEventListener('click', (e) => {
      const target = e.target.closest('a');
      if (target && !this.isTransitioning) {
        const href = target.getAttribute('href');
        if (href && !href.startsWith('#') && !href.startsWith('javascript:')) {
          e.preventDefault();
          this.navigate(href);
        }
      }
    });

    // 初始化页面加载动画
    this.initPageLoadAnimation();
  }

  /**
   * 初始化页面加载动画
   */
  initPageLoadAnimation() {
    // 添加页面加载遮罩
    const loader = document.createElement('div');
    loader.id = 'page-loader';
    loader.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(255, 255, 255, 0.9);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 9999;
      transition: opacity ${this.transitionDuration}ms ease-in-out;
    `;

    // 添加加载动画
    const spinner = document.createElement('div');
    spinner.style.cssText = `
      width: 50px;
      height: 50px;
      border: 5px solid #f3f3f3;
      border-top: 5px solid #3498db;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    `;

    const style = document.createElement('style');
    style.textContent = `
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    `;

    document.head.appendChild(style);
    loader.appendChild(spinner);
    document.body.appendChild(loader);

    // 页面加载完成后隐藏加载动画
    window.addEventListener('load', () => {
      setTimeout(() => {
        loader.style.opacity = '0';
        setTimeout(() => {
          loader.style.display = 'none';
        }, this.transitionDuration);
      }, 500);
    });
  }

  /**
   * 页面导航
   * @param {string} url - 目标URL
   */
  async navigate(url) {
    if (this.isTransitioning) return;

    this.isTransitioning = true;

    try {
      // 显示加载动画
      const loader = document.getElementById('page-loader');
      if (loader) {
        loader.style.display = 'flex';
        loader.style.opacity = '1';
      }

      // 执行页面淡出动画
      await this.fadeOut();

      // 重定向到目标URL
      window.location.href = url;
    } catch (error) {
      console.error('页面导航失败:', error);
      this.isTransitioning = false;
    }
  }

  /**
   * 页面淡出动画
   * @returns {Promise} - 动画完成的Promise
   */
  fadeOut() {
    return new Promise((resolve) => {
      document.body.style.opacity = '1';
      document.body.style.transition = `opacity ${this.transitionDuration}ms ease-in-out`;
      
      setTimeout(() => {
        document.body.style.opacity = '0';
        
        setTimeout(() => {
          resolve();
        }, this.transitionDuration);
      }, 100);
    });
  }

  /**
   * 页面淡入动画
   */
  fadeIn() {
    document.body.style.opacity = '0';
    document.body.style.transition = `opacity ${this.transitionDuration}ms ease-in-out`;
    
    setTimeout(() => {
      document.body.style.opacity = '1';
    }, 100);
  }

  /**
   * 执行带动态效果的重定向
   * @param {string} url - 目标URL
   * @param {string} effect - 动画效果类型：'fade'（淡入淡出）、'slide'（滑动）
   */
  redirectWithEffect(url, effect = 'fade') {
    if (this.isTransitioning) return;

    this.isTransitioning = true;

    switch (effect) {
      case 'slide':
        this.slideOut(url);
        break;
      case 'fade':
      default:
        this.navigate(url);
        break;
    }
  }

  /**
   * 页面滑动动画
   * @param {string} url - 目标URL
   */
  async slideOut(url) {
    try {
      // 显示加载动画
      const loader = document.getElementById('page-loader');
      if (loader) {
        loader.style.display = 'flex';
        loader.style.opacity = '1';
      }

      // 执行页面滑动动画
      document.body.style.transform = 'translateX(0)';
      document.body.style.transition = `transform ${this.transitionDuration}ms ease-in-out`;
      
      setTimeout(() => {
        document.body.style.transform = 'translateX(-100%)';
        
        setTimeout(() => {
          window.location.href = url;
        }, this.transitionDuration);
      }, 100);
    } catch (error) {
      console.error('页面滑动动画失败:', error);
      this.isTransitioning = false;
    }
  }
}

// 初始化页面过渡效果
window.addEventListener('DOMContentLoaded', () => {
  window.pageTransition = new PageTransition();
  // 页面加载时执行淡入动画
  window.pageTransition.fadeIn();
});

// 导出PageTransition类
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PageTransition;
}