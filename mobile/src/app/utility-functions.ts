export function shortName(name: string): string {
  const parts = name.trim().split(" ");
  if (parts.length === 1) return name;
  return `${parts[0]} ${parts[1][0]}`;
}
