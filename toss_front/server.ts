import express from "express";
import cors from "cors";
import path from "path";
import { fileURLToPath } from "url";
import { createServer as createViteServer } from "vite";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const secretKey = 'test_sk_oEjb0gm23Pj7bllxa00nrpGwBJn5';

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));
  app.use(cors());

  // API Routes

    /*
  app.post("/sandbox-dev/api/v1/payments/confirm", async (req, res) => {
    const { paymentKey, orderId, amount } = req.body;

    try {
      const encryptedSecretKey = "Basic " + Buffer.from(secretKey + ":").toString("base64");

      const response = await fetch("https://api.tosspayments.com/v1/payments/confirm", {
        method: "POST",
        headers: {
          Authorization: encryptedSecretKey,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ orderId, amount, paymentKey }),
      });

      const data = await response.json();
      console.log("Confirm Payment Response:", data);

      if (!response.ok) {
        return res.status(response.status).json(data);
      }

      return res.json({ data });
    } catch (error) {
      console.error("Error confirming payment:", error);
      return res.status(500).json({ message: "Internal Server Error" });
    }
  });

  */

  // Vite middleware for development
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
