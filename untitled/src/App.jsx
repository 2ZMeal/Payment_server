import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import CheckoutPage from './pages/CheckoutPage';
import NicePayCallback from './pages/NicePayCallback';

function App() {
  return (
      <Router>
        <Routes>
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/nicepay/callback" element={<NicePayCallback />} />
        </Routes>
      </Router>
  );
}

export default App;
