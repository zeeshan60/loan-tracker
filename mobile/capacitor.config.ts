import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.zeenomlabs.moneyrabbit',
  appName: 'MoneyRabbit',
  webDir: 'www/browser',
  ios: {
    scheme: "App",
  }
};

export default config;
