import { registerRootComponent } from 'expo'; // Importa o registro do Expo para informar qual componente inicia o aplicativo em cada plataforma.

import App from './App'; // Importa o componente principal porque ele contém a navegação e as telas abertas pelo Expo.

// Registra o App como a primeira tela que o Expo deve abrir.
registerRootComponent(App);
