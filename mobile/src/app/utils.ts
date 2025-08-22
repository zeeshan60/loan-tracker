import { Capacitor } from '@capacitor/core';

export const platform = Capacitor.getPlatform();
export const isMobile = Capacitor.isNativePlatform();
export const isWeb = Capacitor.getPlatform() === 'web';
export const isIos = Capacitor.getPlatform() === 'ios'
export const isAndroid = Capacitor.getPlatform() === 'android'

export const isPluginAvailable = (plugin: string) => Capacitor.isPluginAvailable(plugin);
