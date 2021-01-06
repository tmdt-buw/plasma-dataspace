export class Util {

  /**
   * Translates an rgb input string into hex string
   * @param rgbString of a color
   * @returns hex color of the rbg input
   */
  static rgbToHex(rgbString: string): string {
    const rgb = rgbString.split(',');
    const r = parseInt(rgb[0].substring(4), 10);
    const cg = parseInt(rgb[1], 10);
    const b = parseInt(rgb[2], 10);
    return '#' + [r, cg, b].map(x => {
      const hex = x.toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    }).join('');
  }

}
