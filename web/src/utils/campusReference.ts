const referencePoints = [
  [10.8049, 106.7168], [10.7921, 106.7317], [10.8657, 106.6187],
  [10.8332, 106.6533], [10.8698, 106.7176], [10.8738, 106.6369],
];

export function isReferencePoint(latitude: number, longitude: number) {
  return referencePoints.some(([lat, lng]) => lat === latitude && lng === longitude);
}
